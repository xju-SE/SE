package com.xju.sem.common.security;

import org.springframework.stereotype.Component;

/**
 * 供 @PreAuthorize SpEL 调用的认证守卫。role 与 authStatus 正交：
 * 已登录但未认证（UNVERIFIED）在写操作上等价于访客——用 authGuard.isVerified() 拦截。
 *
 * 用法：@PreAuthorize("@authGuard.isVerified()")
 *      @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
 */
@Component("authGuard")
public class AuthGuard {

    public boolean isVerified() {
        LoginUser lu = SecurityUtil.currentOrNull();
        return lu != null && lu.isVerified();
    }

    public boolean isLogin() {
        return SecurityUtil.currentOrNull() != null;
    }
}
