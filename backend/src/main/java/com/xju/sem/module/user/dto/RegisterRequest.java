package com.xju.sem.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册入参。identityType 只能是 STUDENT / ALUMNI（ADMIN 不经公开注册）。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 ≥8 位且同时含字母与数字。 */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "密码需至少8位且同时包含字母与数字")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "身份意向不能为空")
    @Pattern(regexp = "STUDENT|ALUMNI", message = "身份意向只能是 STUDENT 或 ALUMNI")
    private String identityType;
}
