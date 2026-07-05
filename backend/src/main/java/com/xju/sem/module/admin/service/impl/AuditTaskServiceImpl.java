package com.xju.sem.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.admin.dto.AuditTaskBriefDTO;
import com.xju.sem.module.admin.dto.AuditTaskDTO;
import com.xju.sem.module.admin.dto.AuditTaskDetailDTO;
import com.xju.sem.module.admin.dto.AuditTaskQuery;
import com.xju.sem.module.admin.dto.BatchResultDTO;
import com.xju.sem.module.admin.dto.BatchResultItem;
import com.xju.sem.module.admin.dto.ChecklistResult;
import com.xju.sem.module.admin.dto.ContributorCertPayload;
import com.xju.sem.module.admin.dto.PreCheckResultDTO;
import com.xju.sem.module.admin.entity.AuditTask;
import com.xju.sem.module.admin.enums.AuditDecision;
import com.xju.sem.module.admin.enums.AuditTargetType;
import com.xju.sem.module.admin.enums.AuditTaskStatus;
import com.xju.sem.module.admin.enums.ReasonTemplate;
import com.xju.sem.module.admin.enums.ReviewKind;
import com.xju.sem.module.admin.mapper.AuditTaskMapper;
import com.xju.sem.module.admin.mapper.TargetTypeCount;
import com.xju.sem.module.admin.service.AuditTargetHandler;
import com.xju.sem.module.admin.service.AuditTaskService;
import com.xju.sem.module.admin.service.PreCheckService;
import com.xju.sem.module.knowledge.dto.response.KnowledgeBriefDTO;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import com.xju.sem.module.notification.service.NotificationService;
import com.xju.sem.module.opportunity.dto.response.OpportunityBriefDTO;
import com.xju.sem.module.opportunity.service.OpportunityService;
import com.xju.sem.module.profile.dto.response.AlumniBriefDTO;
import com.xju.sem.module.profile.service.AlumniProfileService;
import com.xju.sem.module.user.dto.AuthApplicationDTO;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.service.AuthApplicationService;
import com.xju.sem.module.user.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一审核任务 Service 实现（M7 审核队列范围）。
 *
 * <p><b>分发策略</b>：按 target_type 分发到 {@link AuditTargetHandler}（§6.4/§9 策略路由），
 * 新增 target_type 只需新增 Handler 实现类注册为 Bean，不改动本类主流程。
 *
 * <p><b>事务边界</b>：{@link #decide} 内"audit_task 状态 CAS 更新"与"调用目标模块 Service 变更
 * 目标实体状态"复用同一物理事务（propagation=REQUIRED），任一环节失败整体回滚，避免出现
 * "任务已通过但目标实体仍是审核中"的中间态（§6.4 设计要点）。
 *
 * <p><b>自调用说明</b>：{@link #batchDecide} 内需要 {@link #decide} 逐条各自独立事务提交，
 * 但同一实例内 {@code this.decide(...)} 会绕过 Spring AOP 代理导致 {@code @Transactional} 失效
 * （Spring 官方文档明确的自调用限制），故注入 {@code @Lazy} 自身接口引用 {@link #self} 走代理转发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditTaskServiceImpl implements AuditTaskService {

    private final AuditTaskMapper auditTaskMapper;
    private final PreCheckService preCheckService;
    private final UserService userService;
    private final AuthApplicationService authApplicationService;
    private final KnowledgeEntryService knowledgeEntryService;
    private final NotificationService notificationService;
    private final List<AuditTargetHandler> handlerList;

    // M7 剩余部分补充：OPPORTUNITY 详情/摘要聚合、CONTRIBUTOR_CERT 详情聚合（申请人简介+申请材料）
    private final OpportunityService opportunityService;
    private final AlumniProfileService alumniProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Lazy
    @Autowired
    private AuditTaskService self;

    private Map<String, AuditTargetHandler> handlers;

    @PostConstruct
    void initHandlers() {
        handlers = handlerList.stream().collect(Collectors.toMap(AuditTargetHandler::targetType, h -> h));
    }

    @Override
    @Transactional
    public AuditTaskDTO createTask(String targetType, Long targetId, Long submitterId, String reviewKind) {
        AuditTask task = new AuditTask();
        task.setTargetType(targetType);
        task.setTargetId(targetId);
        task.setSubmitterId(submitterId);
        task.setReviewKind(StringUtils.hasText(reviewKind) ? reviewKind : ReviewKind.NEW.name());
        task.setStatus(AuditTaskStatus.PENDING.name());
        auditTaskMapper.insert(task);
        return toDTO(task);
    }

    @Override
    @Transactional
    public AuditTaskDTO recordAutoApproved(String targetType, Long targetId, Long submitterId, String decisionNote) {
        AuditTask task = new AuditTask();
        task.setTargetType(targetType);
        task.setTargetId(targetId);
        task.setSubmitterId(submitterId);
        task.setReviewKind(ReviewKind.AUTO.name());
        task.setStatus(AuditTaskStatus.AUTO_APPROVED.name());
        task.setDecisionNote(StringUtils.hasText(decisionNote) ? decisionNote : "系统自动核验通过，留痕不进入人工队列");
        task.setDecidedAt(java.time.LocalDateTime.now());
        auditTaskMapper.insert(task);
        return toDTO(task);
    }

    @Override
    @Transactional
    public void runPreCheck(Long taskId, Long knowledgeEntryId) {
        PreCheckResultDTO result = preCheckService.runPreCheck(knowledgeEntryId);
        String json = preCheckService.serialize(result);
        // 仅更新 auto_precheck 一列：MyBatis-Plus updateById 默认按非空字段更新（NOT_NULL 策略），
        // 不会覆盖该行其余列，无需先 selectById 再整体回写
        AuditTask patch = new AuditTask();
        patch.setId(taskId);
        patch.setAutoPrecheck(json);
        auditTaskMapper.updateById(patch);
    }

    @Override
    public PageResult<AuditTaskBriefDTO> pageQueue(AuditTaskQuery query) {
        if (StringUtils.hasText(query.getTargetType()) && !AuditTargetType.isValid(query.getTargetType())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "targetType 取值不合法");
        }
        String status = StringUtils.hasText(query.getStatus()) ? query.getStatus() : AuditTaskStatus.PENDING.name();

        LambdaQueryWrapper<AuditTask> qw = new LambdaQueryWrapper<>();
        qw.eq(AuditTask::getStatus, status);
        if (StringUtils.hasText(query.getTargetType())) {
            qw.eq(AuditTask::getTargetType, query.getTargetType());
        }
        // 紧迫度：等待最久的排最前（FR-M7-01）
        qw.orderByAsc(AuditTask::getCreatedAt);

        IPage<AuditTask> page = auditTaskMapper.selectPage(pageOf(query.getPage(), query.getSize()), qw);
        List<AuditTaskBriefDTO> records = page.getRecords().stream().map(this::toBrief).collect(Collectors.toList());

        if (StringUtils.hasText(query.getKeyword())) {
            // 简化说明：跨模块联合模糊搜索超出课程项目范围，keyword 退化为对已解析的申请人/标题摘要
            // 做当前页内的包含匹配，不追求跨模块联合查询的整体分页精确性（见实现说明"假设与简化"）
            String kw = query.getKeyword().trim().toLowerCase();
            records = records.stream()
                    .filter(r -> containsIgnoreCase(r.getSubmitterName(), kw) || containsIgnoreCase(r.getTargetSummary(), kw))
                    .collect(Collectors.toList());
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public Map<String, Long> countPendingByType() {
        List<TargetTypeCount> counts = auditTaskMapper.countPendingByType();
        return counts.stream().collect(Collectors.toMap(TargetTypeCount::getTargetType, TargetTypeCount::getCnt));
    }

    @Override
    public AuditTaskDetailDTO getById(Long id) {
        AuditTask task = requireExisting(id);
        AuditTaskDetailDTO.AuditTaskDetailDTOBuilder builder = AuditTaskDetailDTO.builder()
                .id(task.getId())
                .targetType(task.getTargetType())
                .targetId(task.getTargetId())
                .reviewKind(task.getReviewKind())
                .status(task.getStatus())
                .submitterId(task.getSubmitterId())
                .reviewerId(task.getReviewerId())
                .decisionNote(task.getDecisionNote())
                .decidedAt(task.getDecidedAt())
                .createdAt(task.getCreatedAt())
                .preCheck(preCheckService.deserialize(task.getAutoPrecheck()));

        if (task.getSubmitterId() != null) {
            builder.submitter(safeUserBrief(task.getSubmitterId()));
        }
        if (AuditTargetType.AUTH_APPLICATION.name().equals(task.getTargetType())) {
            builder.authApplication(safeGetOrNull(() -> authApplicationService.getById(task.getTargetId())));
        } else if (AuditTargetType.KNOWLEDGE_ENTRY.name().equals(task.getTargetType())) {
            builder.knowledgeEntry(safeGetOrNull(() -> knowledgeEntryService.getById(task.getTargetId(), null, true)));
        } else if (AuditTargetType.OPPORTUNITY.name().equals(task.getTargetType())) {
            // viewerIsAdmin=true 绕过 PENDING_REVIEW 仅发布人/ADMIN 可见的限制
            builder.opportunity(safeGetOrNull(() -> opportunityService.getById(task.getTargetId(), null, true)));
        } else if (AuditTargetType.CONTRIBUTOR_CERT.name().equals(task.getTargetType())) {
            builder.contributorApplicant(safeGetOrNull(() -> alumniProfileService.getBrief(task.getTargetId())));
            builder.contributorCertPayload(parseContributorCertPayload(task.getAutoPrecheck()));
        }
        return builder.build();
    }

    @Override
    @Transactional
    public AuditTaskDTO decide(Long id, Long reviewerId, AuditDecision decision, ChecklistResult checklistResult,
                               String reasonCode, String comment) {
        AuditTask task = requireExisting(id);
        if (!AuditTaskStatus.PENDING.name().equals(task.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该审核任务当前状态不允许该操作（非待处理）");
        }
        if (decision == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "decision 不能为空");
        }

        AuditDecision effectiveDecision = decision;
        String effectiveReasonCode = reasonCode;
        String effectiveComment = comment;

        boolean isKnowledgeEntry = AuditTargetType.KNOWLEDGE_ENTRY.name().equals(task.getTargetType());
        if (isKnowledgeEntry && checklistResult != null && checklistResult.anyChecked()) {
            // 三项 checklist 任一勾选 → 强制转退回，忽略 ADMIN 实际传入的 decision（即使误选"通过"也拦截），§6.3
            effectiveDecision = AuditDecision.RETURN;
            effectiveReasonCode = mapChecklistToReasonCode(checklistResult);
            if (!StringUtils.hasText(effectiveComment)) {
                ReasonTemplate rt = ReasonTemplate.ofCode(effectiveReasonCode);
                effectiveComment = rt != null ? rt.getText() : "隐私 checklist 命中，已强制退回";
            }
        }

        AuditTargetHandler handler = handlers.get(task.getTargetType());
        if (handler == null) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该 targetType 暂不支持终审：" + task.getTargetType());
        }

        String noteToStore = buildDecisionNote(effectiveReasonCode, effectiveComment);
        int rows = auditTaskMapper.casDecide(id, AuditTaskStatus.PENDING.name(), effectiveDecision.toStatus(),
                reviewerId, noteToStore);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该审核任务已被处理，请刷新后重试");
        }

        // 与上面 audit_task CAS 更新同一物理事务：目标模块 approve/reject/return 抛异常会整体回滚（§6.4）
        handler.handle(task.getTargetId(), reviewerId, effectiveDecision, effectiveComment);

        notifySubmitter(task.getSubmitterId(), task.getTargetType(), task.getTargetId(), effectiveDecision, effectiveComment);

        AuditTask updated = auditTaskMapper.selectById(id);
        return toDTO(updated);
    }

    @Override
    public BatchResultDTO batchDecide(String targetType, List<Long> ids, Long reviewerId, AuditDecision decision,
                                      String reasonCode, String comment) {
        if (!StringUtils.hasText(targetType)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "targetType 不能为空");
        }
        AuditTargetHandler handler = handlers.get(targetType);
        if (handler == null || !handler.supportsBatch()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该 targetType 不支持批量操作");
        }
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "ids 不能为空");
        }

        List<BatchResultItem> details = new ArrayList<>();
        int success = 0;
        int fail = 0;
        // 逐条独立事务（经由 self 代理调用 decide），不用一个大事务包住整批：单条失败不影响其余条目（§6.9）
        for (Long id : ids) {
            try {
                AuditTask task = auditTaskMapper.selectById(id);
                if (task == null) {
                    throw new BusinessException(ResultCode.NOT_FOUND, "审核任务不存在");
                }
                if (!targetType.equals(task.getTargetType())) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "该任务不属于所选 targetType");
                }
                self.decide(id, reviewerId, decision, null, reasonCode, comment);
                details.add(new BatchResultItem(id, true, "OK"));
                success++;
            } catch (BusinessException e) {
                details.add(new BatchResultItem(id, false, e.getMessage()));
                fail++;
            } catch (Exception e) {
                log.warn("批量审核处理失败 id={}: {}", id, e.getMessage());
                details.add(new BatchResultItem(id, false, "处理失败"));
                fail++;
            }
        }
        return new BatchResultDTO(success, fail, details);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String mapChecklistToReasonCode(ChecklistResult c) {
        // 优先级：hasRealName > hasContact > hasLocatableCombo（§6.3）
        if (c.isHasRealName()) {
            return ReasonTemplate.PRIVACY_REAL_NAME.name();
        }
        if (c.isHasContact()) {
            return ReasonTemplate.PRIVACY_CONTACT.name();
        }
        return ReasonTemplate.PRIVACY_LOCATABLE_COMBO.name();
    }

    /** 拼装落库的 decision_note：[理由码]模板文案 + ADMIN 补充说明，截断至 300 字符适配 schema 列宽。 */
    private String buildDecisionNote(String reasonCode, String comment) {
        ReasonTemplate rt = ReasonTemplate.ofCode(reasonCode);
        StringBuilder sb = new StringBuilder();
        if (rt != null) {
            sb.append('[').append(rt.name()).append(']').append(rt.getText());
        }
        if (StringUtils.hasText(comment)) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(comment);
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.length() > 300 ? sb.substring(0, 300) : sb.toString();
    }

    private void notifySubmitter(Long submitterId, String targetType, Long targetId, AuditDecision decision, String comment) {
        if (submitterId == null) {
            return;
        }
        boolean approved = decision == AuditDecision.APPROVE;
        String title = approved ? "审核已通过" : (decision == AuditDecision.RETURN ? "审核被退回，需补充" : "审核未通过");
        String content = approved
                ? "你提交的" + targetTypeLabel(targetType) + "已通过审核"
                : "你提交的" + targetTypeLabel(targetType) + "未通过审核" + (StringUtils.hasText(comment) ? "：" + comment : "");
        try {
            // type 取值对齐全局契约 NotificationService.send(...) 的 type∈HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM
            notificationService.send(submitterId, "AUDIT_RESULT", title, content, targetType, targetId);
        } catch (Exception e) {
            log.warn("审核结果通知发送失败 targetType={} targetId={}: {}", targetType, targetId, e.getMessage());
        }
    }

    private String targetTypeLabel(String targetType) {
        if (AuditTargetType.AUTH_APPLICATION.name().equals(targetType)) {
            return "身份认证申请";
        }
        if (AuditTargetType.KNOWLEDGE_ENTRY.name().equals(targetType)) {
            return "知识条目候选";
        }
        if (AuditTargetType.OPPORTUNITY.name().equals(targetType)) {
            return "机会发布";
        }
        if (AuditTargetType.CONTRIBUTOR_CERT.name().equals(targetType)) {
            return "贡献者认证申请";
        }
        return "内容";
    }

    /** 仅 CONTRIBUTOR_CERT 使用；解析失败（如 KNOWLEDGE_ENTRY 场景该列实为预检 JSON）返回 null。 */
    private ContributorCertPayload parseContributorCertPayload(String autoPrecheck) {
        if (!StringUtils.hasText(autoPrecheck)) {
            return null;
        }
        try {
            return objectMapper.readValue(autoPrecheck, ContributorCertPayload.class);
        } catch (Exception e) {
            log.warn("贡献者认证申请材料解析失败: {}", e.getMessage());
            return null;
        }
    }

    private AuditTask requireExisting(Long id) {
        AuditTask task = auditTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审核任务不存在");
        }
        return task;
    }

    private Page<AuditTask> pageOf(int page, int size) {
        int p = page <= 0 ? 1 : page;
        int s = size <= 0 ? 10 : Math.min(size, 50);
        return new Page<>(p, s);
    }

    private AuditTaskDTO toDTO(AuditTask t) {
        return AuditTaskDTO.builder()
                .id(t.getId())
                .targetType(t.getTargetType())
                .targetId(t.getTargetId())
                .submitterId(t.getSubmitterId())
                .reviewKind(t.getReviewKind())
                .status(t.getStatus())
                .reviewerId(t.getReviewerId())
                .decisionNote(t.getDecisionNote())
                .decidedAt(t.getDecidedAt())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private AuditTaskBriefDTO toBrief(AuditTask t) {
        String submitterName = null;
        if (t.getSubmitterId() != null) {
            UserBriefDTO brief = safeUserBrief(t.getSubmitterId());
            if (brief != null) {
                submitterName = StringUtils.hasText(brief.getRealName()) ? brief.getRealName() : brief.getUsername();
            }
        }
        String summary = resolveTargetSummary(t.getTargetType(), t.getTargetId());

        boolean alert = false;
        if (AuditTargetType.KNOWLEDGE_ENTRY.name().equals(t.getTargetType())) {
            PreCheckResultDTO pc = preCheckService.deserialize(t.getAutoPrecheck());
            alert = pc != null && (pc.isContactSignal() || pc.isIdNumberSignal());
        }

        return AuditTaskBriefDTO.builder()
                .id(t.getId())
                .targetType(t.getTargetType())
                .targetId(t.getTargetId())
                .reviewKind(t.getReviewKind())
                .status(t.getStatus())
                .submitterId(t.getSubmitterId())
                .submitterName(submitterName)
                .targetSummary(summary)
                .privacyAlert(alert)
                .createdAt(t.getCreatedAt())
                .build();
    }

    private String resolveTargetSummary(String targetType, Long targetId) {
        try {
            if (AuditTargetType.AUTH_APPLICATION.name().equals(targetType)) {
                AuthApplicationDTO app = authApplicationService.getById(targetId);
                String who = StringUtils.hasText(app.getRealName()) ? app.getRealName() : app.getStudentNo();
                return app.getApplyRole() + "认证-" + (who == null ? "" : who);
            }
            if (AuditTargetType.KNOWLEDGE_ENTRY.name().equals(targetType)) {
                KnowledgeBriefDTO brief = knowledgeEntryService.getBrief(targetId);
                return brief.getTitle();
            }
            if (AuditTargetType.OPPORTUNITY.name().equals(targetType)) {
                OpportunityBriefDTO brief = opportunityService.getBrief(targetId);
                return brief.getTitle();
            }
            if (AuditTargetType.CONTRIBUTOR_CERT.name().equals(targetType)) {
                AlumniBriefDTO brief = alumniProfileService.getBrief(targetId);
                return "贡献者认证-" + brief.getNickname();
            }
        } catch (Exception e) {
            log.warn("解析审核任务目标摘要失败 targetType={} targetId={}: {}", targetType, targetId, e.getMessage());
        }
        return null;
    }

    private UserBriefDTO safeUserBrief(Long userId) {
        return safeGetOrNull(() -> userService.getBrief(userId));
    }

    private <T> T safeGetOrNull(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("跨模块只读摘要获取失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean containsIgnoreCase(String haystack, String needleLower) {
        return haystack != null && haystack.toLowerCase().contains(needleLower);
    }
}
