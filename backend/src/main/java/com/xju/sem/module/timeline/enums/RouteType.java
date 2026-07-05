package com.xju.sem.module.timeline.enums;

/**
 * 发展路线枚举（对齐 schema.sql timeline_template.route_type 注释）。
 *
 * <p>语义分两层：{@link #UNDECIDED} 是"未决策通用默认线"，覆盖大一共性阶段，学生无需任何前置
 * 操作即自动生效（懒初始化，见 §6.6）；其余四项是"分化路线"，由学生在大二下/大三上决策窗口
 * 主动选入。{@code UNDECIDED} 不可被用户经"选择/切换路线"接口主动选入（仅系统默认态，见 30603）。
 */
public enum RouteType {

    /** 未决策通用默认线（大一共性阶段）。 */
    UNDECIDED,
    /** 考研。 */
    POSTGRAD,
    /** 就业。 */
    EMPLOY,
    /** 竞赛。 */
    COMPETITION,
    /** 考公。 */
    CIVIL;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (RouteType r : values()) {
            if (r.name().equals(v)) {
                return true;
            }
        }
        return false;
    }

    /** 是否为分化路线（非 UNDECIDED）。 */
    public static boolean isDivergent(String v) {
        return isValid(v) && !UNDECIDED.name().equals(v);
    }
}
