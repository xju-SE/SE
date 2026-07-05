package com.xju.sem.module.admin.enums;

/**
 * audit_task.target_type 取值（entity 字段仍以 String 存储，本枚举仅供 Service 层校验/分支使用）。
 * 四类均已注册生产者事件与终审 Handler：{@link #AUTH_APPLICATION}/{@link #KNOWLEDGE_ENTRY} 见
 * 审核队列初版；{@link #OPPORTUNITY}（{@code OpportunityAuditHandler} + 监听
 * {@code OpportunitySubmittedEvent}）/{@link #CONTRIBUTOR_CERT}（{@code ContributorCertAuditHandler}
 * + {@code ContributorCertService.apply}）由 M7 剩余部分补充注册，新增均只增 Handler 实现类，
 * 未改动 {@code AuditTaskServiceImpl} 主流程（开闭原则）。
 */
public enum AuditTargetType {
    AUTH_APPLICATION,
    KNOWLEDGE_ENTRY,
    OPPORTUNITY,
    CONTRIBUTOR_CERT;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (AuditTargetType t : values()) {
            if (t.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
