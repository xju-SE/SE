package com.xju.sem.module.admin.service.impl;

import com.xju.sem.module.admin.dto.AuditTaskDTO;
import com.xju.sem.module.admin.enums.AuditTargetType;
import com.xju.sem.module.admin.enums.ReviewKind;
import com.xju.sem.module.admin.service.AuditTaskService;
import com.xju.sem.module.knowledge.event.KnowledgeEntrySubmittedEvent;
import com.xju.sem.module.opportunity.event.OpportunitySubmittedEvent;
import com.xju.sem.module.user.event.AuthApplicationSubmittedEvent;
import com.xju.sem.module.user.service.AuthApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * FR-M7-01①：监听目标模块提交审核事件，建 audit_task（AUTO 直接留痕，否则 PENDING），07 详细设计 §6.1。
 *
 * <p>三个监听方法均在源事件所在事务 <b>已提交后</b> 的独立事务内执行
 * （{@code phase = AFTER_COMMIT}），因此本模块创建 audit_task 失败不会回滚已提交的目标模块动作
 * （如"知识候选已创建成功"不应因 audit_task 写入失败而回滚）——失败仅记录日志，走人工补偿重试，
 * 与 M1/M3/M5 已确立的跨模块事件解耦风格一致。
 *
 * <p>{@link #onOpportunitySubmitted}（M7 剩余部分补充）：M5 落地后按地基契约
 * "OpportunitySubmittedEvent(opportunityId,publisherId)" 监听，建 target_type=OPPORTUNITY 的
 * PENDING 任务；终审分发见 {@code OpportunityAuditHandler}（FR-M7-08）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditTaskEventListener {

    private final AuditTaskService auditTaskService;
    private final AuthApplicationService authApplicationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuthApplicationSubmitted(AuthApplicationSubmittedEvent event) {
        try {
            Long submitterId = resolveApplicant(event.getAppId());
            if (event.isAutoApproved()) {
                auditTaskService.recordAutoApproved(AuditTargetType.AUTH_APPLICATION.name(), event.getAppId(),
                        submitterId, "系统自动核验通过（SSO/邀请码），留痕不进入人工队列");
            } else {
                auditTaskService.createTask(AuditTargetType.AUTH_APPLICATION.name(), event.getAppId(),
                        submitterId, ReviewKind.NEW.name());
            }
        } catch (Exception e) {
            log.error("建立认证申请审核任务失败 appId={}: {}", event.getAppId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKnowledgeEntrySubmitted(KnowledgeEntrySubmittedEvent event) {
        try {
            String reviewKind = event.isRevision() ? ReviewKind.REVISION.name() : ReviewKind.NEW.name();
            AuditTaskDTO task = auditTaskService.createTask(AuditTargetType.KNOWLEDGE_ENTRY.name(),
                    event.getEntryId(), event.getAuthorId(), reviewKind);
            // FR-M7-05：任务建立后同步跑自动预检并写回，供人工审核详情页预填 checklist 提示（§6.2）
            auditTaskService.runPreCheck(task.getId(), event.getEntryId());
        } catch (Exception e) {
            log.error("建立知识候选审核任务失败 entryId={}: {}", event.getEntryId(), e.getMessage(), e);
        }
    }

    /**
     * FR-M7-01③（M7 剩余部分补充）：监听机会提交审核事件，建 audit_task(target_type=OPPORTUNITY)。
     * 字段与地基跨模块契约"OpportunitySubmittedEvent(opportunityId,publisherId)"严格一致。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOpportunitySubmitted(OpportunitySubmittedEvent event) {
        try {
            auditTaskService.createTask(AuditTargetType.OPPORTUNITY.name(), event.getOpportunityId(),
                    event.getPublisherId(), ReviewKind.NEW.name());
        } catch (Exception e) {
            log.error("建立机会审核任务失败 opportunityId={}: {}", event.getOpportunityId(), e.getMessage(), e);
        }
    }

    /** M7 仅凭 appId 回读 auth_application 获取申请人 userId，不直接访问 M1 的 Mapper/表。 */
    private Long resolveApplicant(Long appId) {
        try {
            return authApplicationService.getById(appId).getUserId();
        } catch (Exception e) {
            log.warn("回读认证申请人失败 appId={}: {}", appId, e.getMessage());
            return null;
        }
    }
}
