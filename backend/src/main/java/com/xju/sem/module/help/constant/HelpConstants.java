package com.xju.sem.module.help.constant;

/**
 * M4 结构化求助模块常量：登记到全局 notification 表的 type/ref_type 取值。
 *
 * <p>notification.type 取值域受地基契约约束为 {@code HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM}
 * （schema notification.type VARCHAR(16)）。本模块四类事件按语义归入其中：
 * 路由匹配→HELP_MATCH；采纳→ADOPT；收到新回答/追问/系统提示→SYSTEM。
 */
public final class HelpConstants {

    private HelpConstants() {
    }

    /** 通知类型：路由匹配（"有一条你可能能解答的求助"）。 */
    public static final String NOTIFY_HELP_MATCH = "HELP_MATCH";

    /** 通知类型：采纳（回答被采纳 / 求助已采纳）。 */
    public static final String NOTIFY_ADOPT = "ADOPT";

    /** 通知类型：系统提示（收到新回答 / 收到追问 / 超时自动关闭）。 */
    public static final String NOTIFY_SYSTEM = "SYSTEM";

    /** 通知关联对象类型：求助单。 */
    public static final String REF_HELP_TICKET = "HELP_TICKET";

    /** 通知关联对象类型：回答。 */
    public static final String REF_HELP_ANSWER = "HELP_ANSWER";
}
