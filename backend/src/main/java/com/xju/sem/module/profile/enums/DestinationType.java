package com.xju.sem.module.profile.enums;

/**
 * 去向类型（alumni_path_card.destination_type）。schema 列注释核心取值为
 * EMPLOY/POSTGRAD/CIVIL_SERVICE/ABROAD/OTHER；本枚举在其上补充详细设计所列的
 * ENTREPRENEUR/FLEXIBLE（同样以枚举名字符串落库，不改变列类型）。
 *
 * <p>仅 {@link #EMPLOY} 与 {@link #POSTGRAD} 展开分支明细字段（见 §6.1 分支校验），
 * 其余类型只使用通用字段（grad_stage/major/grad_year/grad_gpa/advice）。
 */
public enum DestinationType {
    /** 就业：展开城市/行业/公司/岗位。 */
    EMPLOY,
    /** 深造：展开目标院校/专业/初试成绩/复试/备考时长。 */
    POSTGRAD,
    /** 考公。 */
    CIVIL_SERVICE,
    /** 出国。 */
    ABROAD,
    /** 创业。 */
    ENTREPRENEUR,
    /** 灵活就业。 */
    FLEXIBLE,
    /** 其他。 */
    OTHER;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (DestinationType t : values()) {
            if (t.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmploy(String value) {
        return EMPLOY.name().equals(value);
    }

    public static boolean isPostgrad(String value) {
        return POSTGRAD.name().equals(value);
    }
}
