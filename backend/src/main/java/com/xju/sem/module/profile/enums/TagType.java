package com.xju.sem.module.profile.enums;

/**
 * 全局 tag.tag_type 取值（只读引用，标签体系维护归属 M7）。本模块用于入参标签的类型校验：
 * 目标行业须为 INDUSTRY，成长标签须为 INTEREST/GROWTH，专业须为 MAJOR。
 */
public enum TagType {
    MAJOR,
    GRADE,
    INDUSTRY,
    INTEREST,
    GROWTH,
    QUESTION_TYPE;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (TagType t : values()) {
            if (t.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
