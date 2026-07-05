package com.xju.sem.common.security;

import lombok.Getter;

/**
 * 当前登录用户的上下文（从 JWT 解析后放入 SecurityContext）。
 * role：STUDENT/ALUMNI/ADMIN；authStatus：UNVERIFIED/PENDING/VERIFIED/REJECTED。
 */
@Getter
public class LoginUser {

    private final Long userId;
    private final String role;
    private final String authStatus;

    public LoginUser(Long userId, String role, String authStatus) {
        this.userId = userId;
        this.role = role;
        this.authStatus = authStatus;
    }

    public boolean isVerified() {
        return "VERIFIED".equals(authStatus);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
