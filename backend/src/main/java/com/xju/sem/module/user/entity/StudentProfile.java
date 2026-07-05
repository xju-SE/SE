package com.xju.sem.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 在校生档案（表 student_profile），与 user 一对一。
 * 认证通过后由 M1 写入身份字段；成长画像扩展字段（gpa/target 等）由 M2 读写。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student_profile")
public class StudentProfile extends BaseEntity {

    private Long userId;

    /** 真实姓名（认证材料，不进入 PUBLIC 可见）。 */
    private String realName;

    /** 学号（认证写入后只读）。 */
    private String studentNo;

    private String college;

    /** 专业标签 FK→tag.id (tag_type=MAJOR)，认证终审由 major_text 解析而来。 */
    private Long majorTagId;

    /** 入学年份，如 2023。 */
    private Integer enrollYear;

    /** 年级档 1..10（系统按 enrollYear 每年重算）。 */
    private Integer gradeLevel;

    private BigDecimal gpa;

    /** GPA 满分制 4/5。 */
    private Integer gpaScale;

    private String targetCity;

    private Long targetIndustryTagId;

    private String bio;

    private String avatarUrl;
}
