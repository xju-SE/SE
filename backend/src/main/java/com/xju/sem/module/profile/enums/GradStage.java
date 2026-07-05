package com.xju.sem.module.profile.enums;

/**
 * 校友路径卡对应的毕业阶段/学历（alumni_path_card.grad_stage）。
 * 取值与 schema.sql 列注释严格一致（BACHELOR/MASTER/PHD），entity 以 String 存储，
 * 本枚举仅供 Service 层校验/分支使用，不作为 MyBatis 类型处理器绑定字段类型。
 */
public enum GradStage {
    /** 本科毕业。 */
    BACHELOR,
    /** 硕士毕业。 */
    MASTER,
    /** 博士毕业。 */
    PHD;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (GradStage s : values()) {
            if (s.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
