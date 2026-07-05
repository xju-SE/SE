package com.xju.sem.common.security;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 便捷获取当前登录用户。Service/Controller 中直接调用。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser lu)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return lu;
    }

    public static Long currentUserId() {
        return current().getUserId();
    }

    /** 允许匿名访问的场景下，取不到则返回 null（不抛异常）。 */
    public static LoginUser currentOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu) {
            return lu;
        }
        return null;
    }
}
