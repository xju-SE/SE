package com.xju.sem.module.help.enums;

/**
 * 求助单状态机（对齐 04 详细设计 §4）。数据库以 String 存储，本枚举仅供 Service 层
 * 状态校验与流转判定，不作为 MyBatis 枚举 TypeHandler 持久化。
 *
 * <pre>
 *   OPEN ──路由匹配≥1人──▶ MATCHED ──首条回答──▶ ANSWERED ──采纳──▶ ADOPTED ──▶ CLOSED
 *     │                        │                    │                        ▲
 *     └────────────────────────┴────────────────────┴────── 关闭/撤回/超时 ──┘
 * </pre>
 * CLOSED 为终态，不支持 reopen。
 */
public enum HelpTicketStatus {
    OPEN,
    MATCHED,
    ANSWERED,
    ADOPTED,
    CLOSED;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (HelpTicketStatus s : values()) {
            if (s.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
