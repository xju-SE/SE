package com.xju.sem.module.profile.enums;

/**
 * 深造录取方式（alumni_path_card.postgrad_admission_type，仅 destination_type=POSTGRAD 使用）。
 * 取值与 schema.sql 列注释一致：RECOMMEND（保研/推免）、EXAM（考研/统考）。
 *
 * <p>业务规则（§6.1 分支校验，S7）：destinationType=POSTGRAD 时本字段必填；取值 EXAM 时
 * exam_score（初试成绩构成）为必填，取值 RECOMMEND 时 exam_score 可空。
 */
public enum PostgradAdmissionType {
    /** 保研/推免。 */
    RECOMMEND,
    /** 考研/统考。 */
    EXAM;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (PostgradAdmissionType t : values()) {
            if (t.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExam(String value) {
        return EXAM.name().equals(value);
    }
}
