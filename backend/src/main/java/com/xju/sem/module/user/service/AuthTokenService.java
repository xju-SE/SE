package com.xju.sem.module.user.service;

import com.xju.sem.module.user.dto.LoginResponse;
import com.xju.sem.module.user.dto.TokenPair;

/**
 * 令牌服务：登录签发、刷新、登出（无状态设计，本期不做服务端黑名单）。
 */
public interface AuthTokenService {

    /** BCrypt 校验密码 + 账号状态校验 → 签发 access/refresh，附带 UserDTO。 */
    LoginResponse login(String username, String password);

    /** 注册成功后据 role/authStatus 直接签发令牌对（自动登录）。 */
    TokenPair issueFor(Long userId, String role, String authStatus);

    /** 用 refresh token 换取新的 access token（重载用户最新 role/authStatus）。 */
    TokenPair refresh(String refreshToken);

    /** 登出：无状态，占位（前端丢弃 token）。 */
    void logout(Long userId);
}
