package com.xju.sem.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 统一审核任务（对应 schema.sql {@code audit_task} 表，精确列以该表为准）。
 *
 * <p>与 07 详细设计文档 §3.1 的差异（以 schema.sql 为准的精简，详见实现说明"假设与简化"一节）：
 * 不持有 {@code applicant_id}（改名 {@code submitter_id} 且允许 NULL）、{@code payload}、
 * {@code pre_check_result}（改名 {@code auto_precheck}，VARCHAR(500) 存 JSON 文本而非 JSON 列）、
 * {@code checklist_result}（不单独持久化，命中项已折叠进 {@code decision_note} 文本）、
 * {@code decision}（与 {@code status} 合一，不重复存储）、{@code decision_reason_code}/
 * {@code decision_comment}（合并为单一 {@code decision_note}）、{@code submitted_at}（复用
 * {@link BaseEntity#getCreatedAt()}）；状态机也精简为不含 {@code WITHDRAWN}
 * （见 {@code AuditTaskStatus}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_task")
public class AuditTask extends BaseEntity {

    /** AUTH_APPLICATION/KNOWLEDGE_ENTRY/OPPORTUNITY/CONTRIBUTOR_CERT（本期仅前两者有生产者）。 */
    private String targetType;

    /** 目标记录ID：AUTH_APPLICATION→auth_application.id；KNOWLEDGE_ENTRY→knowledge_entry.id。 */
    private Long targetId;

    /** 申请人/提交人，冗余存储避免按 target_type 分别 JOIN；允许为 NULL（如来源不明的系统触发场景）。 */
    private Long submitterId;

    /** NEW/REVISION/AUTO/NEW_FROM_HELP。 */
    private String reviewKind;

    /** PENDING/APPROVED/REJECTED/RETURNED/AUTO_APPROVED。 */
    private String status;

    /** 系统自动完整性/隐私预检结果（JSON字符串，仅 KNOWLEDGE_ENTRY 写入有意义值），见 PreCheckService。 */
    private String autoPrecheck;

    private Long reviewerId;

    /** 审核意见：折叠了标准理由模板文本 + ADMIN 补充说明，见 AuditTaskServiceImpl#buildDecisionNote。 */
    private String decisionNote;

    private LocalDateTime decidedAt;
}
