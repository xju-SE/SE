package com.xju.sem.module.timeline.enums;

/**
 * 学期阶段枚举（对齐 schema.sql timeline_node.stage 注释：{@code GRADE1_1/GRADE1_2/.../GRADE4_2}），
 * 覆盖本科四年共 8 学期。每个阶段携带三项日历元数据，供 {@code TimelineCalendarUtil} 由学生
 * {@code enrollYear} 换算绝对建议时点与当前所处学期（§6.1）：
 * <ul>
 *   <li>{@code order}：自然学期先后（1..8），用于按学期分组渲染与"上一学期"回溯；</li>
 *   <li>{@code fall}：true=秋季学期（9 月开学），false=春季学期（次年 2/3 月开学）；</li>
 *   <li>{@code yearOffset}：该学期"特征月份"所落在的自然年相对 enrollYear 的偏移
 *       （秋季=9 月所在年；春季=2~7 月所在年）。</li>
 * </ul>
 *
 * <p>内容编排约定（Service 层软提示、非硬校验）：{@code UNDECIDED} 通用线节点落在
 * {@link #GRADE1_1}~{@link #GRADE2_1}（大一~大二上共性阶段）；四条分化线节点落在
 * {@link #GRADE2_2}~{@link #GRADE4_2}（决策窗口之后）。二者学期区间不重叠，使"当前生效路线"
 * 可由 user_progress 关联模板反查（§6.3），无需为"当前路线"单独建表。
 */
public enum Stage {

    GRADE1_1(1, true, 0, "大一上"),
    GRADE1_2(2, false, 1, "大一下"),
    GRADE2_1(3, true, 1, "大二上"),
    GRADE2_2(4, false, 2, "大二下"),
    GRADE3_1(5, true, 2, "大三上"),
    GRADE3_2(6, false, 3, "大三下"),
    GRADE4_1(7, true, 3, "大四上"),
    GRADE4_2(8, false, 4, "大四下");

    private final int order;
    private final boolean fall;
    private final int yearOffset;
    private final String label;

    Stage(int order, boolean fall, int yearOffset, String label) {
        this.order = order;
        this.fall = fall;
        this.yearOffset = yearOffset;
        this.label = label;
    }

    public int getOrder() {
        return order;
    }

    public boolean isFall() {
        return fall;
    }

    public int getYearOffset() {
        return yearOffset;
    }

    public String getLabel() {
        return label;
    }

    /** 大一~大二上共性阶段：此区间恒定使用 UNDECIDED 通用线（§6.3）。 */
    public boolean isCommonPhase() {
        return this == GRADE1_1 || this == GRADE1_2 || this == GRADE2_1;
    }

    /** 上一个自然学期；GRADE1_1 无上一学期返回 null。供补救优先级"学期邻近度"加成（§6.5）。 */
    public Stage previous() {
        for (Stage s : values()) {
            if (s.order == this.order - 1) {
                return s;
            }
        }
        return null;
    }

    public static boolean isValid(String v) {
        return from(v) != null;
    }

    /** 解析字符串为枚举，非法返回 null。 */
    public static Stage from(String v) {
        if (v == null) {
            return null;
        }
        for (Stage s : values()) {
            if (s.name().equals(v)) {
                return s;
            }
        }
        return null;
    }
}
