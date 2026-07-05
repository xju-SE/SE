package com.xju.sem.module.admin.enums;

/**
 * report.target_type 取值（对齐 schema.sql 列注释：HELP_TICKET/KNOWLEDGE_ENTRY/ALUMNI_PATH_CARD/
 * OPPORTUNITY/USER）。举报范围收敛到公开可读内容与账号本身，不含 TEAM（07 详细设计 §1"明确不做"：
 * M5 team 缺少既成的 ADMIN 强制下线能力，纠纷本期人工线下协调）。
 */
public enum ReportTargetType {
    HELP_TICKET,
    KNOWLEDGE_ENTRY,
    ALUMNI_PATH_CARD,
    OPPORTUNITY,
    USER;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (ReportTargetType t : values()) {
            if (t.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
