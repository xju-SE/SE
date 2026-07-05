package com.xju.sem.module.opportunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.opportunity.dto.request.CreateOpportunityRequest;
import com.xju.sem.module.opportunity.dto.request.OpportunityQuery;
import com.xju.sem.module.opportunity.dto.request.UpdateOpportunityRequest;
import com.xju.sem.module.opportunity.dto.response.OpportunityBriefDTO;
import com.xju.sem.module.opportunity.dto.response.OpportunityDTO;
import com.xju.sem.module.opportunity.entity.Opportunity;
import com.xju.sem.module.opportunity.enums.OpportunityStatus;
import com.xju.sem.module.opportunity.enums.OpportunityType;
import com.xju.sem.module.opportunity.event.OpportunitySubmittedEvent;
import com.xju.sem.module.opportunity.mapper.OpportunityMapper;
import com.xju.sem.module.opportunity.service.OpportunityService;
import com.xju.sem.module.opportunity.service.TeamService;
import com.xju.sem.module.notification.service.NotificationService;
import com.xju.sem.module.user.constant.Role;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 机会 Service 实现：CRUD、状态机流转（§4.1）、终审门（§6.1）、报名信令简化版（§6.3）。
 *
 * <p><b>并发控制</b>：opportunity 无 version 列，状态类流转（终审/手动结束/强制下线/定时推进）
 * 统一走 {@link OpportunityMapper} 的状态 CAS，内容编辑走归属校验的普通更新，与 M1/M3/M4 先例
 * 同一分工原则。跨模块只调 {@link UserService}/{@link NotificationService}/{@link TeamService}
 * 接口，不直连其表/Mapper。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityServiceImpl implements OpportunityService {

    private final OpportunityMapper opportunityMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final TeamService teamService;

    @Value("${sem.opportunity.closing-soon-hours:72}")
    private int closingSoonHours;

    private static final String REF_TYPE = "OPPORTUNITY";

    @Override
    @Transactional
    public OpportunityDTO create(Long publisherId, CreateOpportunityRequest request) {
        if (!OpportunityType.isValid(request.getType())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "机会类型取值不合法");
        }
        Role role = userService.getRole(publisherId);
        if (role != Role.ALUMNI && role != Role.ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅校友/管理员可发布机会");
        }
        if (!userService.isVerified(publisherId)) {
            throw new BusinessException(ResultCode.NOT_VERIFIED, "身份未认证，无法发布机会");
        }
        boolean referral = Boolean.TRUE.equals(request.getIsReferral());
        validateReferralRule(referral, role, request.getType());

        Opportunity opp = new Opportunity();
        opp.setType(request.getType());
        opp.setTitle(request.getTitle());
        opp.setDescription(request.getDescription());
        opp.setDeadline(request.getDeadline());
        opp.setPublisherId(publisherId);
        opp.setIsReferral(referral ? 1 : 0);
        opp.setTeamRequired(Boolean.TRUE.equals(request.getTeamRequired()) ? 1 : 0);
        opp.setStatus(referral ? OpportunityStatus.PENDING_REVIEW.name() : computeStatusByTime(request.getDeadline()));
        opportunityMapper.insert(opp);

        if (referral) {
            // 事务内发布，监听方以 AFTER_COMMIT 消费（M7 侧待补充，见类注释）
            eventPublisher.publishEvent(new OpportunitySubmittedEvent(opp.getId(), publisherId));
        }
        return toDTO(opp, publisherId, role == Role.ADMIN);
    }

    @Override
    @Transactional
    public OpportunityDTO update(Long id, Long operatorId, boolean isAdmin, UpdateOpportunityRequest request) {
        Opportunity opp = requireExisting(id);
        if (!isAdmin && !operatorId.equals(opp.getPublisherId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权编辑该机会");
        }
        if (OpportunityStatus.ENDED.name().equals(opp.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "已结束的机会不可编辑");
        }
        if (!OpportunityType.isValid(request.getType())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "机会类型取值不合法");
        }
        boolean referral = Boolean.TRUE.equals(request.getIsReferral());
        Role publisherRole = userService.getRole(opp.getPublisherId());
        validateReferralRule(referral, publisherRole, request.getType());

        boolean wasReferral = Integer.valueOf(1).equals(opp.getIsReferral());
        boolean wasPendingReview = OpportunityStatus.PENDING_REVIEW.name().equals(opp.getStatus());
        boolean wasRejected = OpportunityStatus.REJECTED.name().equals(opp.getStatus());

        opp.setType(request.getType());
        opp.setTitle(request.getTitle());
        opp.setDescription(request.getDescription());
        opp.setDeadline(request.getDeadline());
        opp.setIsReferral(referral ? 1 : 0);
        opp.setTeamRequired(Boolean.TRUE.equals(request.getTeamRequired()) ? 1 : 0);

        // 已公开的普通机会被改成内推（is_referral 0→1）必须回到 PENDING_REVIEW 重新终审，
        // 不得停留在公开态绕过 M7；避免编辑动作成为审核旁路。
        boolean needReReview = referral && !wasReferral && !wasPendingReview && !wasRejected;
        // S18：REJECTED 的机会允许编辑，编辑提交后一律回 PENDING_REVIEW 重新终审，
        // 解除"被拒不可重提"的限制（不再是终态死路）。
        boolean needResubmit = needReReview || wasRejected;
        if (wasPendingReview || needResubmit) {
            opp.setStatus(OpportunityStatus.PENDING_REVIEW.name());
        } else {
            // 已发布且未触发重新审核：按新 deadline 重算对外状态（§6.2 计算规则复用）
            opp.setStatus(computeStatusByTime(request.getDeadline()));
        }
        opportunityMapper.updateById(opp);

        if (needResubmit) {
            // 事务内发布，M7 以 AFTER_COMMIT 消费建 audit_task（与 create 内推分支一致；
            // REJECTED 重提交同样需要生成新的终审任务）
            eventPublisher.publishEvent(new OpportunitySubmittedEvent(opp.getId(), opp.getPublisherId()));
        }
        return toDTO(opp, operatorId, isAdmin);
    }

    @Override
    public OpportunityDTO getById(Long id, Long viewerUserId, boolean viewerIsAdmin) {
        Opportunity opp = requireExisting(id);
        boolean isOwner = viewerUserId != null && viewerUserId.equals(opp.getPublisherId());
        boolean isPrivateStatus = OpportunityStatus.PENDING_REVIEW.name().equals(opp.getStatus())
                || OpportunityStatus.REJECTED.name().equals(opp.getStatus());
        if (isPrivateStatus && !viewerIsAdmin && !isOwner) {
            // 审核中/被拒的机会不进入公开状态机，非发布人/ADMIN 一律按不存在处理，与 M3 同一模式
            throw new BusinessException(ResultCode.NOT_FOUND, "机会不存在");
        }
        return toDTO(opp, viewerUserId, viewerIsAdmin);
    }

    @Override
    public OpportunityBriefDTO getBrief(Long id) {
        return toBrief(requireExisting(id));
    }

    @Override
    public PageResult<OpportunityBriefDTO> list(OpportunityQuery query, Long viewerUserId) {
        if (StringUtils.hasText(query.getType()) && !OpportunityType.isValid(query.getType())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "机会类型取值不合法");
        }
        if (StringUtils.hasText(query.getKeyword()) && query.getKeyword().length() > 200) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "关键字长度不能超过200");
        }
        List<String> privateStatuses = List.of(OpportunityStatus.PENDING_REVIEW.name(), OpportunityStatus.REJECTED.name());
        QueryWrapper<Opportunity> qw = new QueryWrapper<>();
        qw.and(w -> {
            w.notIn("status", privateStatuses);
            if (viewerUserId != null) {
                w.or(w2 -> w2.in("status", privateStatuses).eq("publisher_id", viewerUserId));
            }
        });
        if (query.isClosingSoon()) {
            qw.eq("status", OpportunityStatus.CLOSING_SOON.name());
        } else if (!query.isIncludeEnded()) {
            qw.notIn("status", List.of(OpportunityStatus.CLOSED.name(), OpportunityStatus.ENDED.name()));
        }
        if (StringUtils.hasText(query.getType())) {
            qw.eq("type", query.getType());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            qw.like("title", query.getKeyword().trim());
        }
        qw.last("ORDER BY FIELD(status,'CLOSING_SOON','ONGOING','PENDING_REVIEW','REJECTED','CLOSED','ENDED'), deadline ASC");
        IPage<Opportunity> result = opportunityMapper.selectPage(pageOf(query.getPage(), query.getSize()), qw);
        return toBriefPage(result);
    }

    @Override
    public PageResult<OpportunityBriefDTO> listClosingSoon(int limit) {
        OpportunityQuery q = new OpportunityQuery();
        q.setClosingSoon(true);
        q.setPage(1);
        q.setSize(limit);
        return list(q, null);
    }

    @Override
    @Transactional
    public void approve(Long id, Long reviewerId) {
        Opportunity opp = requireExisting(id);
        if (!OpportunityStatus.PENDING_REVIEW.name().equals(opp.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅待审核的机会可终审通过");
        }
        String newStatus = computeStatusByTime(opp.getDeadline());
        int rows = opportunityMapper.casStatus(id, OpportunityStatus.PENDING_REVIEW.name(), newStatus);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "审核状态已变更，请刷新后重试");
        }
        notifySafe(opp.getPublisherId(), "机会审核通过",
                "你发布的机会《" + opp.getTitle() + "》已通过审核，现已对外公开", id);
    }

    @Override
    @Transactional
    public void reject(Long id, Long reviewerId, String reason) {
        Opportunity opp = requireExisting(id);
        if (!OpportunityStatus.PENDING_REVIEW.name().equals(opp.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅待审核的机会可终审拒绝");
        }
        // S18：拒绝落到独立 REJECTED 态（不再直接归档 ENDED），理由随通知下发+日志留痕（schema 无
        // review_comment 列）；发布人可通过 update() 编辑后重新提交回 PENDING_REVIEW 重新终审。
        int rows = opportunityMapper.casStatus(id, OpportunityStatus.PENDING_REVIEW.name(), OpportunityStatus.REJECTED.name());
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "审核状态已变更，请刷新后重试");
        }
        log.info("opportunity {} rejected by reviewer {}, reason={}", id, reviewerId, reason);
        notifySafe(opp.getPublisherId(), "机会审核未通过",
                "你发布的机会《" + opp.getTitle() + "》未通过审核："
                        + (StringUtils.hasText(reason) ? reason : "请修改后重新发布"), id);
    }

    @Override
    @Transactional
    public OpportunityDTO end(Long id, Long operatorId, boolean isAdmin, String reason) {
        Opportunity opp = requireExisting(id);
        if (!isAdmin && !operatorId.equals(opp.getPublisherId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权结束该机会");
        }
        if (OpportunityStatus.ENDED.name().equals(opp.getStatus())) {
            if (isAdmin) {
                return toDTO(opp, operatorId, true); // 治理强制下线幂等
            }
            throw new BusinessException(ResultCode.STATE_CONFLICT, "机会已结束");
        }
        int rows = opportunityMapper.casStatus(id, opp.getStatus(), OpportunityStatus.ENDED.name());
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "机会状态已变更，请刷新后重试");
        }
        opp.setStatus(OpportunityStatus.ENDED.name());
        log.info("opportunity {} ended by operator {} (isAdmin={}), reason={}", id, operatorId, isAdmin, reason);
        teamService.endAllByOpportunity(id, "OPPORTUNITY_ENDED"); // FR-M5-10 级联
        notifySafe(opp.getPublisherId(), "机会已结束",
                "你发布的机会《" + opp.getTitle() + "》已结束"
                        + (StringUtils.hasText(reason) ? "：" + reason : ""), id);
        return toDTO(opp, operatorId, isAdmin);
    }

    @Override
    public void applySignal(Long id, Long userId) {
        Opportunity opp = requireExisting(id);
        if (!OpportunityStatus.ONGOING.name().equals(opp.getStatus())
                && !OpportunityStatus.CLOSING_SOON.name().equals(opp.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该机会当前不可报名（未开放或已截止）");
        }
        // schema 无 apply_count 列，简化为校验 + 通知发布人，不做热度计数持久化（见类/接口注释）
        log.info("opportunity {} apply signal by user {}", id, userId);
        notifySafe(opp.getPublisherId(), "机会收到新的报名意向",
                "有用户对你发布的机会《" + opp.getTitle() + "》表示报名意向", id);
    }

    @Override
    public void delete(Long id, Long operatorId, boolean isAdmin) {
        Opportunity opp = requireExisting(id);
        if (!isAdmin && !operatorId.equals(opp.getPublisherId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己发布的机会");
        }
        opportunityMapper.deleteById(id);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** 内推入口仅对"已认证 ALUMNI 发布的实习类机会"开放（05 详细设计 §6.1），ADMIN 代发不受此限。 */
    private void validateReferralRule(boolean referral, Role role, String type) {
        if (referral && role != Role.ADMIN
                && (role != Role.ALUMNI || !OpportunityType.INTERNSHIP.name().equals(type))) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "仅已认证校友发布的实习类机会可标记为内推类");
        }
    }

    /** 按 deadline 与当前时间计算对外状态：deadline 已过→CLOSED；临近阈值→CLOSING_SOON；否则 ONGOING。 */
    private String computeStatusByTime(LocalDateTime deadline) {
        LocalDateTime now = LocalDateTime.now();
        if (deadline == null || !deadline.isAfter(now)) {
            return OpportunityStatus.CLOSED.name();
        }
        if (deadline.isBefore(now.plusHours(closingSoonHours))) {
            return OpportunityStatus.CLOSING_SOON.name();
        }
        return OpportunityStatus.ONGOING.name();
    }

    private Opportunity requireExisting(Long id) {
        Opportunity opp = opportunityMapper.selectById(id);
        if (opp == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "机会不存在");
        }
        return opp;
    }

    private Page<Opportunity> pageOf(Integer page, Integer size) {
        int p = (page == null || page <= 0) ? 1 : page;
        int s = (size == null || size <= 0) ? 10 : Math.min(size, 50);
        return new Page<>(p, s);
    }

    private void notifySafe(Long userId, String title, String content, Long oppId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, "SYSTEM", title, content, REF_TYPE, oppId);
        } catch (Exception e) {
            log.warn("机会{}通知发送失败: {}", oppId, e.getMessage());
        }
    }

    private String publisherName(Long publisherId) {
        try {
            UserBriefDTO brief = userService.getBrief(publisherId);
            if (brief == null) {
                return null;
            }
            return StringUtils.hasText(brief.getRealName()) ? brief.getRealName() : brief.getUsername();
        } catch (Exception e) {
            log.debug("获取发布人摘要失败 publisherId={}: {}", publisherId, e.getMessage());
            return null;
        }
    }

    private OpportunityDTO toDTO(Opportunity o, Long viewerUserId, boolean viewerIsAdmin) {
        boolean isOwner = viewerUserId != null && viewerUserId.equals(o.getPublisherId());
        boolean isEnded = OpportunityStatus.ENDED.name().equals(o.getStatus());
        return OpportunityDTO.builder()
                .id(o.getId())
                .type(o.getType())
                .title(o.getTitle())
                .description(o.getDescription())
                .deadline(o.getDeadline())
                .status(o.getStatus())
                .publisherId(o.getPublisherId())
                .publisherName(publisherName(o.getPublisherId()))
                .isReferral(o.getIsReferral() != null && o.getIsReferral() == 1)
                .teamRequired(o.getTeamRequired() != null && o.getTeamRequired() == 1)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .editable((viewerIsAdmin || isOwner) && !isEnded)
                .deletable(viewerIsAdmin || isOwner)
                .pendingReview(OpportunityStatus.PENDING_REVIEW.name().equals(o.getStatus()))
                .build();
    }

    private OpportunityBriefDTO toBrief(Opportunity o) {
        return OpportunityBriefDTO.builder()
                .id(o.getId())
                .type(o.getType())
                .title(o.getTitle())
                .status(o.getStatus())
                .deadline(o.getDeadline())
                .publisherId(o.getPublisherId())
                .publisherName(publisherName(o.getPublisherId()))
                .isReferral(o.getIsReferral() != null && o.getIsReferral() == 1)
                .teamRequired(o.getTeamRequired() != null && o.getTeamRequired() == 1)
                .createdAt(o.getCreatedAt())
                .build();
    }

    private PageResult<OpportunityBriefDTO> toBriefPage(IPage<Opportunity> page) {
        List<OpportunityBriefDTO> records = page.getRecords().stream().map(this::toBrief).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
