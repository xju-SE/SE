package com.xju.sem.module.admin.enums;

/**
 * 标签类型（对齐 schema.sql {@code tag.tag_type} 列注释：MAJOR/GRADE/INDUSTRY/INTEREST/GROWTH/
 * QUESTION_TYPE）。entity 字段仍以 String 存储，本枚举仅供 Service 层校验/分支使用。
 */
public enum TagType {
    /** 专业。 */
    MAJOR,
    /** 年级。 */
    GRADE,
    /** 行业。 */
    INDUSTRY,
    /** 兴趣。 */
    INTEREST,
    /** 成长（M6 时间线相关标签，预留）。 */
    GROWTH,
    /** 问题类型（M4 求助分类）。 */
    QUESTION_TYPE;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (TagType t : values()) {
            if (t.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
