package com.xju.sem.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.help.dto.AnswerContentDTO;
import com.xju.sem.module.help.service.HelpAnswerService;
import com.xju.sem.module.knowledge.dto.request.CreateKnowledgeEntryRequest;
import com.xju.sem.module.knowledge.dto.request.OfflineRequest;
import com.xju.sem.module.knowledge.dto.request.UpdateKnowledgeEntryRequest;
import com.xju.sem.module.knowledge.dto.response.KnowledgeBriefDTO;
import com.xju.sem.module.knowledge.dto.response.KnowledgeEntryDTO;
import com.xju.sem.module.knowledge.entity.KnowledgeEntry;
import com.xju.sem.module.knowledge.enums.KnowledgeCategory;
import com.xju.sem.module.knowledge.enums.KnowledgeEntryStatus;
import com.xju.sem.module.knowledge.enums.SourceType;
import com.xju.sem.module.knowledge.event.KnowledgeEntrySubmittedEvent;
import com.xju.sem.module.knowledge.mapper.KnowledgeEntryMapper;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import com.xju.sem.module.knowledge.validator.ExternalLinkValidator;
import com.xju.sem.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识条目 Service 实现：CRUD、状态机流转（§4/§6.2）、认领（§6.4）、来源转化（§6.3）。
 *
 * <p><b>并发控制</b>：内容编辑（update）与状态流转（submit/approve/return/claim/offline）统一
 * 复用 knowledge_entry.version 乐观锁——{@link com.xju.sem.common.config.MybatisPlusConfig} 已注册
 * 全局 {@code OptimisticLockerInnerInterceptor}，任何 updateById 都会自动附加 version 条件；
 * 03 详细设计要求的"状态类更新单独走状态 CAS、不占用乐观锁字段"在本实现中简化为同一把锁，
 * 效果等价（并发写入下后者必然因 version 不一致而失败，抛 {@link OptimisticLockingFailureException}，
 * 由 {@link com.xju.sem.common.exception.GlobalExceptionHandler} 统一转 {@link ResultCode#OPTIMISTIC_LOCK}），
 * 不重复造一套单独的 CAS SQL，见实现说明"假设与简化"一节。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeEntryServiceImpl implements KnowledgeEntryService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final ExternalLinkValidator externalLinkValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final HelpAnswerService helpAnswerService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public KnowledgeEntryDTO create(Long authorId, CreateKnowledgeEntryRequest request) {
        validateCategoryAndLink(request.getCategory(), request.getExternalUrl());

        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(request.getTitle());
        entry.setContent(request.getContent());
        entry.setCategory(request.getCategory());
        entry.setAuthorId(authorId);
        entry.setApplicableScope(request.getApplicableScope());
        entry.setValidUntil(request.getValidUntil());
        entry.setExternalUrl(request.getExternalUrl());
        entry.setStatus(KnowledgeEntryStatus.CANDIDATE.name());
        entry.setSourceType(SourceType.ORIGINAL.name());
        entry.setViewCount(0);
        entry.setVersion(0);
        knowledgeEntryMapper.insert(entry);
        return toDTO(entry, authorId, false);
    }

    @Override
    @Transactional
    public Long createFromHelpAdoption(Long helpTicketId, Long helpAnswerId, Long authorId) {
        if (knowledgeEntryMapper.countBySourceHelpId(helpTicketId) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE, "该求助单已生成过知识候选");
        }
        AnswerContentDTO answer = helpAnswerService.getForCandidate(helpAnswerId);
        if (answer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "来源求助单回答不存在");
        }

        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(StringUtils.hasText(answer.getTicketTitle())
                ? truncate(answer.getTicketTitle(), 150)
                : "求助采纳知识候选#" + helpTicketId);
        entry.setContent(assembleContent(answer));
        // 求助单问题类型标签→分类的映射依赖标签数据，AnswerContentDTO 契约未携带该字段，
        // 默认落 LIFE，由作者在 CANDIDATE 阶段自行改为正确分类（见实现说明"假设与简化"）。
        entry.setCategory(KnowledgeCategory.LIFE.name());
        entry.setAuthorId(authorId);
        entry.setSourceType(SourceType.FROM_HELP.name());
        entry.setSourceHelpId(helpTicketId);
        entry.setStatus(KnowledgeEntryStatus.CANDIDATE.name());
        entry.setViewCount(0);
        entry.setVersion(0);
        knowledgeEntryMapper.insert(entry);

        // 采纳后自动提交审核而非停留 CANDIDATE，避免"采纳了但没人点提交"导致候选流失（§6.3）
        doSubmit(entry, authorId, false);
        return entry.getId();
    }

    @Override
    public KnowledgeEntryDTO getById(Long id, Long viewerUserId, boolean viewerIsAdmin) {
        KnowledgeEntry entry = requireExisting(id);
        boolean isOwner = viewerUserId != null
                && (viewerUserId.equals(entry.getAuthorId()) || viewerUserId.equals(entry.getClaimerId()));
        boolean isPublished = KnowledgeEntryStatus.PUBLISHED.name().equals(entry.getStatus());
        if (!isPublished && !viewerIsAdmin && !isOwner) {
            // 非 PUBLISHED 仅作者/认领人/ADMIN 可见，其余一律按不存在处理（不暴露资源存在性）
            throw new BusinessException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        knowledgeEntryMapper.incrementViewCount(id);
        return toDTO(entry, viewerUserId, viewerIsAdmin);
    }

    @Override
    public KnowledgeBriefDTO getBrief(Long id) {
        KnowledgeEntry entry = requireExisting(id);
        return toBrief(entry);
    }

    @Override
    public boolean existsPublished(Long id) {
        KnowledgeEntry entry = knowledgeEntryMapper.selectById(id);
        return entry != null && KnowledgeEntryStatus.PUBLISHED.name().equals(entry.getStatus());
    }

    @Override
    public PageResult<KnowledgeBriefDTO> list(String category, Long viewerUserId, int page, int size) {
        if (StringUtils.hasText(category) && !KnowledgeCategory.isValid(category)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "分类取值不合法");
        }
        QueryWrapper<KnowledgeEntry> qw = new QueryWrapper<>();
        Long viewer = viewerUserId;
        qw.and(w -> {
            w.eq("status", KnowledgeEntryStatus.PUBLISHED.name());
            if (viewer != null) {
                w.or(w2 -> w2.eq("author_id", viewer));
                w.or(w2 -> w2.eq("claimer_id", viewer));
            }
        });
        if (StringUtils.hasText(category)) {
            qw.eq("category", category);
        }
        qw.orderByDesc("created_at").orderByDesc("view_count");
        IPage<KnowledgeEntry> result = knowledgeEntryMapper.selectPage(pageOf(page, size), qw);
        return toBriefPage(result);
    }

    @Override
    public PageResult<KnowledgeBriefDTO> search(String keyword, String category, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "关键字不能为空");
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 200) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "关键字长度不能超过200");
        }
        if (StringUtils.hasText(category) && !KnowledgeCategory.isValid(category)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "分类取值不合法");
        }
        IPage<KnowledgeEntry> result = trimmed.length() < 2
                ? knowledgeEntryMapper.likeSearch(pageOf(page, size), trimmed, category)
                : knowledgeEntryMapper.fullTextSearch(pageOf(page, size), trimmed, category);
        return toBriefPage(result);
    }

    @Override
    public PageResult<KnowledgeBriefDTO> pageMine(Long userId, String status, int page, int size) {
        QueryWrapper<KnowledgeEntry> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("author_id", userId).or().eq("claimer_id", userId));
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        qw.orderByDesc("updated_at");
        IPage<KnowledgeEntry> result = knowledgeEntryMapper.selectPage(pageOf(page, size), qw);
        return toBriefPage(result);
    }

    @Override
    @Transactional
    public KnowledgeEntryDTO update(Long id, Long userId, boolean isAdmin, UpdateKnowledgeEntryRequest request) {
        KnowledgeEntry entry = requireExisting(id);
        if (!isAdmin && !userId.equals(entry.getAuthorId()) && !userId.equals(entry.getClaimerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权编辑该知识条目");
        }
        if (KnowledgeEntryStatus.REVIEWING.name().equals(entry.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "审核排队中不可编辑，请等待审核结果或联系管理员");
        }
        validateCategoryAndLink(request.getCategory(), request.getExternalUrl());

        boolean wasPublished = KnowledgeEntryStatus.PUBLISHED.name().equals(entry.getStatus());

        entry.setTitle(request.getTitle());
        entry.setContent(request.getContent());
        entry.setCategory(request.getCategory());
        entry.setApplicableScope(request.getApplicableScope());
        entry.setValidUntil(request.getValidUntil());
        entry.setExternalUrl(request.getExternalUrl());
        entry.setVersion(request.getVersion());
        if (wasPublished) {
            // "修订"视为轻量重审，不回退到 CANDIDATE 从头排队（§6.2）
            entry.setStatus(KnowledgeEntryStatus.REVIEWING.name());
        }
        // CANDIDATE：保持 CANDIDATE；EXPIRED/OFFLINE：保持不变，仍需调用 submit 接口才进入 REVIEWING

        checkedUpdate(entry);
        if (wasPublished) {
            eventPublisher.publishEvent(new KnowledgeEntrySubmittedEvent(entry.getId(), entry.getAuthorId(), true));
        }
        return toDTO(entry, userId, isAdmin);
    }

    @Override
    @Transactional
    public KnowledgeEntryDTO submitForReview(Long id, Long userId, boolean isAdmin) {
        KnowledgeEntry entry = requireExisting(id);
        if (!isAdmin && !userId.equals(entry.getAuthorId()) && !userId.equals(entry.getClaimerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权提交审核");
        }
        String status = entry.getStatus();
        boolean isRevision;
        if (KnowledgeEntryStatus.CANDIDATE.name().equals(status)) {
            isRevision = false;
        } else if (KnowledgeEntryStatus.EXPIRED.name().equals(status)
                || KnowledgeEntryStatus.OFFLINE.name().equals(status)) {
            isRevision = true;
        } else {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "当前状态不允许提交审核");
        }
        doSubmit(entry, entry.getAuthorId(), isRevision);
        return toDTO(entry, userId, isAdmin);
    }

    @Override
    @Transactional
    public void approve(Long entryId, Long reviewerId) {
        KnowledgeEntry entry = requireExisting(entryId);
        if (!KnowledgeEntryStatus.REVIEWING.name().equals(entry.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅审核中的条目可终审通过");
        }
        entry.setStatus(KnowledgeEntryStatus.PUBLISHED.name());
        checkedUpdate(entry);
        notifySafe(entry.getAuthorId(), "知识条目已通过审核",
                "你贡献的知识条目《" + entry.getTitle() + "》已通过审核并发布", entryId);
    }

    @Override
    @Transactional
    public void returnToCandidate(Long entryId, Long reviewerId, String reason) {
        KnowledgeEntry entry = requireExisting(entryId);
        if (!KnowledgeEntryStatus.REVIEWING.name().equals(entry.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅审核中的条目可退回");
        }
        entry.setStatus(KnowledgeEntryStatus.CANDIDATE.name());
        checkedUpdate(entry);
        notifySafe(entry.getAuthorId(), "知识条目审核未通过",
                "你贡献的知识条目《" + entry.getTitle() + "》被退回："
                        + (StringUtils.hasText(reason) ? reason : "请补充完善后重新提交"),
                entryId);
    }

    @Override
    @Transactional
    public KnowledgeEntryDTO claim(Long id, Long userId) {
        KnowledgeEntry entry = requireExisting(id);
        String status = entry.getStatus();
        if (!KnowledgeEntryStatus.PUBLISHED.name().equals(status)
                && !KnowledgeEntryStatus.EXPIRED.name().equals(status)
                && !KnowledgeEntryStatus.OFFLINE.name().equals(status)) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "当前状态无需认领");
        }
        if (entry.getClaimerId() != null && !entry.getClaimerId().equals(userId)) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该条目已被他人认领");
        }
        if (!userId.equals(entry.getClaimerId())) {
            Long originalAuthor = entry.getAuthorId();
            entry.setClaimerId(userId);
            checkedUpdate(entry);
            if (!userId.equals(originalAuthor)) {
                notifySafe(originalAuthor, "知识条目已被认领维护",
                        "你的知识条目《" + entry.getTitle() + "》已被认领维护", id);
            }
        }
        return toDTO(entry, userId, false);
    }

    @Override
    @Transactional
    public KnowledgeEntryDTO offline(Long id, Long operatorId, boolean isAdmin, OfflineRequest request) {
        KnowledgeEntry entry = requireExisting(id);
        if (!isAdmin && !operatorId.equals(entry.getAuthorId()) && !operatorId.equals(entry.getClaimerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权下线该知识条目");
        }
        if (!isAdmin) {
            String status = entry.getStatus();
            if (!KnowledgeEntryStatus.PUBLISHED.name().equals(status)
                    && !KnowledgeEntryStatus.EXPIRED.name().equals(status)) {
                throw new BusinessException(ResultCode.STATE_CONFLICT, "当前状态不允许下线");
            }
        }
        entry.setStatus(KnowledgeEntryStatus.OFFLINE.name());
        checkedUpdate(entry);
        if (request != null && StringUtils.hasText(request.getReason())) {
            log.info("knowledge_entry {} offline by operator {}, reason={}", id, operatorId, request.getReason());
        }
        return toDTO(entry, operatorId, isAdmin);
    }

    @Override
    @Transactional
    public void delete(Long id, Long operatorId, boolean isAdmin) {
        KnowledgeEntry entry = requireExisting(id);
        if (isAdmin) {
            knowledgeEntryMapper.deleteById(id);
            return;
        }
        if (!operatorId.equals(entry.getAuthorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己贡献的知识条目");
        }
        if (KnowledgeEntryStatus.PUBLISHED.name().equals(entry.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "已发布的条目不可直接删除，请先下线");
        }
        knowledgeEntryMapper.deleteById(id);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void validateCategoryAndLink(String category, String externalUrl) {
        if (!KnowledgeCategory.isValid(category)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "分类取值不合法");
        }
        externalLinkValidator.validate(category, externalUrl);
    }

    /** 内部提交：置 REVIEWING 并发布提交事件；createFromHelpAdoption 与 submitForReview 共用。 */
    private void doSubmit(KnowledgeEntry entry, Long authorId, boolean isRevision) {
        entry.setStatus(KnowledgeEntryStatus.REVIEWING.name());
        checkedUpdate(entry);
        eventPublisher.publishEvent(new KnowledgeEntrySubmittedEvent(entry.getId(), authorId, isRevision));
    }

    /** updateById 并显式检查受影响行数，0 行说明 version 已不匹配（并发写冲突）。 */
    private void checkedUpdate(KnowledgeEntry entry) {
        int rows = knowledgeEntryMapper.updateById(entry);
        if (rows == 0) {
            throw new OptimisticLockingFailureException("知识条目已被他人修改，请刷新后重试");
        }
    }

    private String assembleContent(AnswerContentDTO answer) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(answer.getPrecondition())) {
            sb.append("【适用前提】").append(answer.getPrecondition()).append("\n\n");
        }
        sb.append("【操作步骤】").append(answer.getSteps() == null ? "" : answer.getSteps());
        if (StringUtils.hasText(answer.getCautions())) {
            sb.append("\n\n【注意事项】").append(answer.getCautions());
        }
        return sb.toString();
    }

    private KnowledgeEntry requireExisting(Long id) {
        KnowledgeEntry entry = knowledgeEntryMapper.selectById(id);
        if (entry == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        return entry;
    }

    private Page<KnowledgeEntry> pageOf(int page, int size) {
        int p = page <= 0 ? 1 : page;
        int s = size <= 0 ? 10 : Math.min(size, 50);
        return new Page<>(p, s);
    }

    private void notifySafe(Long userId, String title, String content, Long entryId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, "SYSTEM", title, content, "KNOWLEDGE_ENTRY", entryId);
        } catch (Exception e) {
            log.warn("知识条目{}通知发送失败: {}", entryId, e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private KnowledgeEntryDTO toDTO(KnowledgeEntry e, Long viewerUserId, boolean viewerIsAdmin) {
        boolean isOwner = viewerUserId != null
                && (viewerUserId.equals(e.getAuthorId()) || viewerUserId.equals(e.getClaimerId()));
        boolean editable = (viewerIsAdmin || isOwner) && !KnowledgeEntryStatus.REVIEWING.name().equals(e.getStatus());
        boolean claimable = viewerUserId != null
                && (KnowledgeEntryStatus.PUBLISHED.name().equals(e.getStatus())
                    || KnowledgeEntryStatus.EXPIRED.name().equals(e.getStatus())
                    || KnowledgeEntryStatus.OFFLINE.name().equals(e.getStatus()))
                && (e.getClaimerId() == null || e.getClaimerId().equals(viewerUserId));
        boolean deletable = viewerIsAdmin
                || (viewerUserId != null && viewerUserId.equals(e.getAuthorId())
                    && !KnowledgeEntryStatus.PUBLISHED.name().equals(e.getStatus()));
        return KnowledgeEntryDTO.builder()
                .id(e.getId())
                .title(e.getTitle())
                .content(e.getContent())
                .category(e.getCategory())
                .authorId(e.getAuthorId())
                .claimerId(e.getClaimerId())
                .applicableScope(e.getApplicableScope())
                .validUntil(e.getValidUntil())
                .externalUrl(e.getExternalUrl())
                .status(e.getStatus())
                .sourceType(e.getSourceType())
                .sourceHelpId(e.getSourceHelpId())
                .viewCount(e.getViewCount())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .editable(editable)
                .claimable(claimable)
                .deletable(deletable)
                .build();
    }

    private KnowledgeBriefDTO toBrief(KnowledgeEntry e) {
        return KnowledgeBriefDTO.builder()
                .id(e.getId())
                .title(e.getTitle())
                .category(e.getCategory())
                .status(e.getStatus())
                .sourceType(e.getSourceType())
                .authorId(e.getAuthorId())
                .claimerId(e.getClaimerId())
                .applicableScope(e.getApplicableScope())
                .validUntil(e.getValidUntil())
                .externalUrl(e.getExternalUrl())
                .viewCount(e.getViewCount())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .relevance(e.getRelevance())
                .build();
    }

    private PageResult<KnowledgeBriefDTO> toBriefPage(IPage<KnowledgeEntry> page) {
        List<KnowledgeBriefDTO> records = page.getRecords().stream().map(this::toBrief).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
