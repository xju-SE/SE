package com.xju.sem.module.admin.enums;

import lombok.Getter;

/**
 * 标准理由模板（07 详细设计 §9）：不建表，纯后端常量，随代码迭代。审核意见展示为
 * "[枚举名]模板文案" + ADMIN 补充说明的拼接，见 AuditTaskServiceImpl#buildDecisionNote。
 */
@Getter
public enum ReasonTemplate {
    PRIVACY_REAL_NAME("检测到可能包含真实姓名，请脱敏后重新提交"),
    PRIVACY_CONTACT("检测到可能包含手机号/邮箱/微信QQ等联系方式，请删除后重新提交"),
    PRIVACY_LOCATABLE_COMBO("检测到多项信息组合可反向定位到个人（如班级+姓名+宿舍号等），请脱敏后重新提交"),
    INCOMPLETE_FIELD("结构化字段填写不完整，请补充后重新提交"),
    OUT_OF_SCOPE("内容不属于本类目治理范围，请调整后重新提交"),
    DUPLICATE("与已发布内容高度重复"),
    OTHER("见管理员补充说明");

    private final String text;

    ReasonTemplate(String text) {
        this.text = text;
    }

    /** 按理由码取模板，非法/为空返回 null（由调用方决定是否降级为纯 comment）。 */
    public static ReasonTemplate ofCode(String code) {
        if (code == null) {
            return null;
        }
        try {
            return ReasonTemplate.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
