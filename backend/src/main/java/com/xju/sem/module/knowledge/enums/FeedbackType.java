package com.xju.sem.module.knowledge.enums;

/** 三态评价类型：有用 / 已过时 / 需更新。UK(entry_id,user_id) 保证一人一条当前有效评价。 */
public enum FeedbackType {
    USEFUL,
    OUTDATED,
    NEED_UPDATE;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (FeedbackType t : values()) {
            if (t.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
