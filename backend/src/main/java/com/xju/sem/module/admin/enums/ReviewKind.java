package com.xju.sem.module.admin.enums;

/**
 * audit_task.review_kind，与 schema.sql 列注释一致。
 */
public enum ReviewKind {
    /** 新申请/新候选。 */
    NEW,
    /** 内容修订重审（如已发布知识条目发起修订）。 */
    REVISION,
    /** 自动初审通过留痕，仅 AUTH_APPLICATION 使用，不进入人工队列。 */
    AUTO,
    /** 求助采纳自动生成的知识候选（预留：当前 M3 KnowledgeEntrySubmittedEvent 未携带来源标志，
     *  仅区分 isRevision，故暂未产生该值，见实现说明"假设与简化"）。 */
    NEW_FROM_HELP
}
