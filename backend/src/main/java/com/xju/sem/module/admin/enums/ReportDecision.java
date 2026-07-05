package com.xju.sem.module.admin.enums;

/** ADMIN 举报处理动作（FR-M7-11）。UPHELD=举报成立，DISMISSED=举报不成立。 */
public enum ReportDecision {
    UPHELD,
    DISMISSED;

    public String toStatus() {
        return this == UPHELD ? ReportStatus.HANDLED.name() : ReportStatus.DISMISSED.name();
    }
}
