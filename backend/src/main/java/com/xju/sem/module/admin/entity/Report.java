package com.xju.sem.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 举报（表 {@code report}）。含逻辑删除与审计时间（继承 {@link BaseEntity}）。
 *
 * <p>与 07 详细设计 §3.2 的差异（以 schema.sql 为准的精简，详见实现说明"假设与简化"）：
 * 无独立 {@code reason_type}/{@code evidence_urls}/{@code handle_action} 列——理由分类编码折叠进
 * {@code reason} 文本前缀，处置动作码折叠进 {@code handle_note} 文本前缀（与 {@code audit_task}
 * 折叠 {@code decision_note} 同一处理思路）；{@code status} 三态 PENDING/HANDLED/DISMISSED
 * （07 文档 UPHELD 对应本表 HANDLED）；无独立 {@code handled_at} 列，处理时间以
 * {@link BaseEntity#getUpdatedAt()} 近似（该列由数据库 ON UPDATE 自动维护）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report")
public class Report extends BaseEntity {

    /** HELP_TICKET/KNOWLEDGE_ENTRY/ALUMNI_PATH_CARD/OPPORTUNITY/USER，见 {@code ReportTargetType}。 */
    private String targetType;

    private Long targetId;

    private Long reporterId;

    /** "[理由码] 举报说明" 折叠文本，见 {@code ReportReasonType}/{@code ReportServiceImpl}。 */
    private String reason;

    /** PENDING/HANDLED/DISMISSED，见 {@code ReportStatus}。 */
    private String status;

    private Long handlerId;

    /** "[处置动作码] 处理说明" 折叠文本，见 {@code ReportHandleAction}/{@code ReportServiceImpl}。 */
    private String handleNote;
}
