package com.xju.sem.module.knowledge.event;

import lombok.Getter;

/**
 * 知识条目提交审核事件。由 {@code KnowledgeEntryServiceImpl} 在提交审核的事务内发布
 * （submitForReview 手动提交 / createFromHelpAdoption 内部自动提交 / update 发起修订三处触发）。
 * M7 监听本事件（{@code @TransactionalEventListener(phase = AFTER_COMMIT)}）创建
 * audit_task(target_type=KNOWLEDGE_ENTRY)，与 M1 AuthApplicationSubmittedEvent 同一解耦模式。
 *
 * <p>isRevision 语义：CANDIDATE→REVIEWING（含 FROM_HELP 首次自动提交）为 false（NEW）；
 * PUBLISHED/EXPIRED/OFFLINE→REVIEWING（内容已曾发布过，属于修订/修复）为 true（REVISION）。
 */
@Getter
public class KnowledgeEntrySubmittedEvent {

    private final Long entryId;
    private final Long authorId;
    private final boolean isRevision;

    public KnowledgeEntrySubmittedEvent(Long entryId, Long authorId, boolean isRevision) {
        this.entryId = entryId;
        this.authorId = authorId;
        this.isRevision = isRevision;
    }
}
