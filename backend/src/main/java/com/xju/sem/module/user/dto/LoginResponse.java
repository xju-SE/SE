package com.xju.sem.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录结果：令牌对 + 用户信息摘要。 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserDTO user;
}
