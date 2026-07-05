package com.xju.sem.module.user.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 当前用户信息出参：user 账号字段 + 对应 profile 摘要（按 role 填充其一分支）。
 * 强隐私字段（realName/studentNo）仅在本人 /users/me 场景回显，跨用户浏览走 M2 的可见性裁剪。
 */
@Data
public class UserDTO {

    private Long userId;
    private String username;
    private String role;
    private String authStatus;
    private String status;
    private String contactVisibility;
    private String profileVisibility;

    // ---- 公共 profile 摘要（认证后才有 profile 记录）----
    private String realName;
    private String college;
    private Long majorTagId;
    private String avatarUrl;
    private String bio;

    // ---- STUDENT 分支 ----
    private String studentNo;
    private Integer enrollYear;
    private Integer gradeLevel;
    private BigDecimal gpa;
    private Integer gpaScale;

    // ---- ALUMNI 分支 ----
    private Integer gradYear;
    private String degreeType;
    private Integer isContributorBadge;
    private Integer helpedCount;
    private Integer adoptedCount;
}
