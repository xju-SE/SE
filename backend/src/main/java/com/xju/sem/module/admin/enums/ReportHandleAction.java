package com.xju.sem.module.admin.enums;

/**
 * 举报成立后的处置动作（07 详细设计 §3.2）。schema.sql 的 {@code report} 无独立 {@code
 * handle_action} 列，本枚举编码折叠进 {@code handle_note} 文本前缀（"[CODE] 说明"），与
 * {@code audit_task.decision_note} 折叠理由码同一处理思路，见 {@code ReportServiceImpl}。
 * 每个动作码只对应固定的 {@code target_type}（见 {@code ReportServiceImpl#requireActionMatch}）。
 */
public enum ReportHandleAction {
    /** 不作自动处置（仅记录成立，人工线下跟进）。 */
    NONE,
    /** 内容隐藏（ALUMNI_PATH_CARD）。 */
    CONTENT_HIDDEN,
    /** 内容下线（KNOWLEDGE_ENTRY/OPPORTUNITY）。 */
    CONTENT_OFFLINE,
    /** 账号封禁（USER）。 */
    USER_DISABLED;

    public static ReportHandleAction ofCodeOrNone(String code) {
        if (code == null) {
            return NONE;
        }
        try {
            return ReportHandleAction.valueOf(code);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
