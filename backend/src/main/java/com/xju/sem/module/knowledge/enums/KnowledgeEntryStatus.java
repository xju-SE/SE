package com.xju.sem.module.knowledge.enums;

/**
 * 知识条目生命周期状态机（entity.status 仍以 String 存储，本枚举仅供 Service 层判断/流转使用）。
 *
 * <pre>
 * CANDIDATE --submit--> REVIEWING --approve--> PUBLISHED
 * REVIEWING --return--> CANDIDATE
 * PUBLISHED --发起修订(update)--> REVIEWING
 * PUBLISHED --valid_until到期(定时任务)--> EXPIRED
 * PUBLISHED --offline--> OFFLINE
 * EXPIRED --编辑后submit--> REVIEWING
 * OFFLINE --编辑后submit--> REVIEWING
 * </pre>
 * 无绝对终态：EXPIRED/OFFLINE 均可经认领人编辑修订回到 REVIEWING 重新流转；
 * 真正的记录终结通过与 status 正交的 deleted 软删标记实现。
 */
public enum KnowledgeEntryStatus {
    CANDIDATE,
    REVIEWING,
    PUBLISHED,
    EXPIRED,
    OFFLINE
}
