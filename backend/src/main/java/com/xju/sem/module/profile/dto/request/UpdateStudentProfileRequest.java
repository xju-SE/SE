package com.xju.sem.module.profile.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 编辑在校生画像入参（PUT /student-profiles/me，FR-M2-01）。
 * 专业/年级/学号为认证结果，不在本请求内，服务端不接受其修改。
 * gpaScale 与 gpa 的上界关系（gpa ≤ gpaScale）在 Service 层交叉校验（20201）。
 */
@Data
public class UpdateStudentProfileRequest {

    @DecimalMin(value = "0.0", message = "GPA 不能为负")
    private BigDecimal gpa;

    /** GPA 满分制，仅允许 4 或 5（Service 层校验）。 */
    private Integer gpaScale;

    @Size(max = 50, message = "目标城市过长")
    private String targetCity;

    /** 目标行业标签，须为 tag_type=INDUSTRY 的有效标签（Service 层校验）。 */
    private Long targetIndustryTagId;

    @Size(max = 500, message = "简介不超过500字")
    private String bio;

    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;
}
