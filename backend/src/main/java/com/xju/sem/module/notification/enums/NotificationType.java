package com.xju.sem.module.notification.enums;

/**
 * notification.type 取值域（entity 字段仍以 String 存储，本枚举仅供 Service 层校验/展示用）。
 * 值域受地基契约约束为固定四类，不像 audit_task.target_type 那样预留后续扩展位——新增触发场景
 * 时应归入既有四类中最贴切的一类（如"收到新回答"归 SYSTEM），而不是新增第五个 type 取值。
 */
public enum NotificationType {

    /** 求助-校友路由匹配（M4）。 */
    HELP_MATCH,

    /** 采纳类（回答被采纳 / 认领知识条目等，M3/M4）。 */
    ADOPT,

    /** 审核结果（认证终审、知识候选终审等，M1/M3/M7 经由 M7 统一发出）。 */
    AUDIT_RESULT,

    /** 系统提示（担保确认、追问、知识条目过期、机会/团队进度等其余场景兜底）。 */
    SYSTEM;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (NotificationType t : values()) {
            if (t.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
