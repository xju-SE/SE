package com.xju.sem.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 注册结果：新账号 id + 自动登录令牌对。 */
@Data
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String accessToken;
    private String refreshToken;
}
