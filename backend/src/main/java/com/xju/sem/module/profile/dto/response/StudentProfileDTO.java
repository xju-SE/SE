package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 在校生画像出参（跨模块契约）：{@code StudentProfileService.getProfile(Long)} 的返回类型，
 * 同时用作 GET/PUT /student-profiles/me 的响应体（P04）。
 *
 * <p>real_name 属强隐私字段：仅在<b>本人 /me 场景</b>由 Controller 决定是否透出；
 * 跨模块调用方拿到本 DTO 时不得把 realName 二次曝光给他人（对齐 real_name 永不 PUBLIC 红线）。
 */
@Data
@Builder
public class StudentProfileDTO {

    private Long userId;

    /** 强隐私，仅本人可见。 */
    private String realName;

    /** 认证写入后只读。 */
    private String studentNo;

    private String college;

    private Long majorTagId;

    private Integer enrollYear;

    /** 年级档 1..10（系统重算，非用户手填）。 */
    private Integer gradeLevel;

    private BigDecimal gpa;

    private Integer gpaScale;

    private String targetCity;

    private Long targetIndustryTagId;

    private String bio;

    private String avatarUrl;

    /** 成长标签（tag_type∈{INTEREST,GROWTH}）。 */
    private List<TagDTO> tags;
}
