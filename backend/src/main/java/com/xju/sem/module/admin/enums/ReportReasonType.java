package com.xju.sem.module.admin.enums;

/**
 * 举报理由分类（07 详细设计 §3.2）。schema.sql 的 {@code report} 只有单一 {@code reason}
 * VARCHAR(300) 列（无独立 {@code reason_type} 列），本枚举编码折叠进 {@code reason} 文本前缀
 * （"[CODE] 说明"），与 {@code ReportHandleAction} 同一处理思路，见 {@code ReportServiceImpl}。
 */
public enum ReportReasonType {
    /** 隐私泄露。 */
    PRIVACY_LEAK,
    /** 虚假信息。 */
    FALSE_INFO,
    /** 不良内容。 */
    INAPPROPRIATE_CONTENT,
    /** 广告骚扰。 */
    SPAM_AD,
    /** 其他。 */
    OTHER;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (ReportReasonType t : values()) {
            if (t.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
