package com.xju.sem.module.opportunity.event;

import lombok.Getter;

/**
 * 机会提交审核事件。由 {@code OpportunityServiceImpl.create/update} 在 {@code isReferral=1}
 * 导致机会进入 PENDING_REVIEW 的事务内发布。M7 侧监听本事件（{@code @TransactionalEventListener(
 * phase = AFTER_COMMIT)}）创建 audit_task(target_type=OPPORTUNITY)，与 M1
 * AuthApplicationSubmittedEvent / M3 KnowledgeEntrySubmittedEvent 同一解耦模式（见 07/08 设计文档
 * "暂未监听 OpportunitySubmittedEvent，待其事件类交付后补充监听方法即可"）。
 *
 * <p>字段与地基跨模块契约"OpportunitySubmittedEvent(opportunityId,publisherId)"严格一致，
 * 供 M7 侧新增 {@code OpportunityAuditHandler} 时直接使用，不在本模块内消费。
 */
@Getter
public class OpportunitySubmittedEvent {

    private final Long opportunityId;
    private final Long publisherId;

    public OpportunitySubmittedEvent(Long opportunityId, Long publisherId) {
        this.opportunityId = opportunityId;
        this.publisherId = publisherId;
    }
}
