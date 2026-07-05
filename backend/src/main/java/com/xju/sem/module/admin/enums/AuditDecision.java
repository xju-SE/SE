package com.xju.sem.module.admin.enums;

/** ADMIN 终审动作，与 audit_task.status 目标值的映射见 {@link #toStatus()}。 */
public enum AuditDecision {
    APPROVE,
    RETURN,
    REJECT;

    public String toStatus() {
        return switch (this) {
            case APPROVE -> AuditTaskStatus.APPROVED.name();
            case RETURN -> AuditTaskStatus.RETURNED.name();
            case REJECT -> AuditTaskStatus.REJECTED.name();
        };
    }
}
