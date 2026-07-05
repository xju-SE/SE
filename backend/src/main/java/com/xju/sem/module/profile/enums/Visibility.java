package com.xju.sem.module.profile.enums;

/**
 * 字段级可见级别（path_visibility.visibility）。语义与全局
 * {@code user.contact_visibility/profile_visibility} 的 SELF/SAME_MAJOR/PUBLIC 口径一致，
 * 但作用对象是路径卡的字段分组而非账号整体，故在本模块内独立声明。
 *
 * <p><b>红线</b>：GUEST（未登录）在任何级别下均不可见；强隐私字段 real_name 永不进入 PUBLIC
 * （real_name 不落在路径卡表内，脱敏时任何对外 DTO 都不携带 real_name，见 §6.2）。
 */
public enum Visibility {
    /** 仅本人可见（对他人恒隐藏）。 */
    SELF,
    /** 同专业登录用户可见。 */
    SAME_MAJOR,
    /** 全平台登录用户可见（仍不含 GUEST）。 */
    PUBLIC;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (Visibility v : values()) {
            if (v.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
