package com.xju.sem.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 三态评价/纠错（对应 schema.sql {@code knowledge_feedback} 表）。
 * UK(entry_id, user_id)：每个用户对每条条目仅保留一条当前有效评价，重复提交按 upsert 处理。
 * 不加乐观锁字段，其并发一致性由唯一约束 + upsert 语义保证（见实现说明 §submitFeedback）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_feedback")
public class KnowledgeFeedback extends BaseEntity {

    /** 被评价的知识条目。 */
    private Long entryId;

    /** 评价人。 */
    private Long userId;

    /** USEFUL/OUTDATED/NEED_UPDATE。 */
    private String feedbackType;

    /** 纠错说明/建议。 */
    private String comment;
}
