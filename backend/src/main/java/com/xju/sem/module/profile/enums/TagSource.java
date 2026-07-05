package com.xju.sem.module.profile.enums;

/**
 * user_tag.tag_source 取值（schema：SELF/SYSTEM）。本模块的成长标签维护一律写 SELF（本人自选），
 * SYSTEM（系统按行为打标）预留给未来行为分析任务。
 */
public enum TagSource {
    SELF,
    SYSTEM;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (TagSource s : values()) {
            if (s.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
