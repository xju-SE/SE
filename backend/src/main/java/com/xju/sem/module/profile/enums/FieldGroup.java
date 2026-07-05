package com.xju.sem.module.profile.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 路径卡字段分组（path_visibility.field_group，VARCHAR(24)）。可见性以"分组"为最小粒度，
 * 而非逐列，既降低配置负担又与 schema 的 path_visibility 一行一组对齐。
 *
 * <p>每个分组覆盖的物理列（alumni_path_card）：
 * <ul>
 *   <li>{@link #BASIC}：grad_stage/major_tag_id/grad_year/grad_gpa（通用必选）</li>
 *   <li>{@link #EMPLOY_LOCATION}：city/industry_tag_id</li>
 *   <li>{@link #EMPLOY_DETAIL}：company/position</li>
 *   <li>{@link #POSTGRAD_TARGET}：target_school/target_major</li>
 *   <li>{@link #POSTGRAD_SCORE}：exam_score（初试成绩构成）</li>
 *   <li>{@link #POSTGRAD_INTERVIEW}：interview_exp（复试经历）</li>
 *   <li>{@link #POSTGRAD_PREP}：prep_months（备考时长）</li>
 *   <li>{@link #ADVICE}：advice（经验总结/建议，通用必选）</li>
 * </ul>
 */
public enum FieldGroup {
    BASIC,
    EMPLOY_LOCATION,
    EMPLOY_DETAIL,
    POSTGRAD_TARGET,
    POSTGRAD_SCORE,
    POSTGRAD_INTERVIEW,
    POSTGRAD_PREP,
    ADVICE;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (FieldGroup g : values()) {
            if (g.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按去向类型返回该卡"实际存在"的字段分组（§6.2 existingFieldGroups / §3.6 只建相关分组）。
     * BASIC 与 ADVICE 为所有去向的通用分组；EMPLOY / POSTGRAD 追加各自分支分组；
     * 其余去向类型（考公/出国/创业/灵活就业/其他）不展开分支明细，仅通用两组。
     */
    public static List<FieldGroup> groupsFor(String destinationType) {
        if (DestinationType.isEmploy(destinationType)) {
            return Arrays.asList(BASIC, EMPLOY_LOCATION, EMPLOY_DETAIL, ADVICE);
        }
        if (DestinationType.isPostgrad(destinationType)) {
            return Arrays.asList(BASIC, POSTGRAD_TARGET, POSTGRAD_SCORE,
                    POSTGRAD_INTERVIEW, POSTGRAD_PREP, ADVICE);
        }
        return Arrays.asList(BASIC, ADVICE);
    }

    /** 该分组是否属于给定去向类型的合法分组（用于可见性配置入参校验，非法抛 20203）。 */
    public static boolean isAllowedFor(String fieldGroup, String destinationType) {
        for (FieldGroup g : groupsFor(destinationType)) {
            if (g.name().equals(fieldGroup)) {
                return true;
            }
        }
        return false;
    }
}
