package com.xju.sem.module.admin.enums;

/**
 * audit_task.status 状态机取值，与 schema.sql 列注释严格一致。
 *
 * <pre>
 * [*] --&gt; PENDING       : 目标模块提交审核事件
 * [*] --&gt; AUTO_APPROVED : 自动初审通过留痕（仅 AUTH_APPLICATION，不进入人工队列）
 * PENDING --&gt; APPROVED  : ADMIN终审通过
 * PENDING --&gt; RETURNED  : ADMIN退回补充
 * PENDING --&gt; REJECTED  : ADMIN终审拒绝
 * </pre>
 * 全部为终态：不支持"重新打开"，目标实体重新提交会产生新一条 audit_task 记录，天然形成
 * 完整的多轮审核历史留痕。schema.sql 未开 WITHDRAWN 列值，故本期不处理"源申请被撤回联动关闭
 * 任务"场景（见实现说明"假设与简化"）。
 */
public enum AuditTaskStatus {
    PENDING,
    APPROVED,
    REJECTED,
    RETURNED,
    AUTO_APPROVED
}
