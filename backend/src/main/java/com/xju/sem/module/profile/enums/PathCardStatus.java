package com.xju.sem.module.profile.enums;

/**
 * 校友路径卡状态机（alumni_path_card.status，以 String 存储）。见实现说明 §4 状态图。
 *
 * <pre>
 * [*]      --创建-->        DRAFT
 * DRAFT    --本人发布-->    PUBLISHED
 * PUBLISHED--本人撤回编辑--> DRAFT
 * PUBLISHED--举报下架(M7)--> HIDDEN
 * DRAFT    --举报下架(M7)--> HIDDEN
 * HIDDEN   --复核恢复(M7)--> PUBLISHED
 * </pre>
 * 约束：HIDDEN 只能由 ADMIN 恢复，本人不可从 HIDDEN 直接改回 PUBLISHED（防绕过治理）。
 * 仅 PUBLISHED 参与浏览列表（§6.2）与聚合统计（§6.3）。
 */
public enum PathCardStatus {
    DRAFT,
    PUBLISHED,
    HIDDEN;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (PathCardStatus s : values()) {
            if (s.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
