package com.xju.sem.module.help.event;

import lombok.Getter;

/**
 * 回答被采纳事件（跨模块闭环枢纽事件）。字段签名与地基契约严格一致：
 * {@code HelpAnswerAdoptedEvent(helpTicketId, helpAnswerId, authorId)}。
 *
 * <p>由 {@code HelpAnswerServiceImpl.adopt} 在采纳事务内发布；本模块自身的
 * {@code HelpAnswerAdoptedListener} 以 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 监听，
 * 事务提交后调用 M3 {@code KnowledgeEntryService.createFromHelpAdoption} 生成知识候选，并回写
 * help_answer.knowledge_entry_id。放到 AFTER_COMMIT 是为了：即使 M3/通知等下游发生故障，也仅记日志
 * 走补偿，绝不回滚"采纳"这一用户已完成的动作（低耦合，见 §9 事务边界）。
 */
@Getter
public class HelpAnswerAdoptedEvent {

    private final Long helpTicketId;
    private final Long helpAnswerId;
    /** 回答人 user.id，作为知识候选的作者。 */
    private final Long authorId;

    public HelpAnswerAdoptedEvent(Long helpTicketId, Long helpAnswerId, Long authorId) {
        this.helpTicketId = helpTicketId;
        this.helpAnswerId = helpAnswerId;
        this.authorId = authorId;
    }
}
