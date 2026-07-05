package com.xju.sem.module.user.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Refresh token 签发/解析。与地基 {@code JwtUtil}（access token）分离，
 * 使用同一密钥、更长有效期（sem.jwt.refresh-expire-days），并带 typ=refresh 声明以区分。
 * 不改动地基共享类，避免影响其它模块。
 */
@Component
public class RefreshTokenProvider {

    @Value("${sem.jwt.secret}")
    private String secret;

    @Value("${sem.jwt.refresh-expire-days:7}")
    private long refreshExpireDays;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpireDays * 24L * 60 * 60 * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("typ", "refresh")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    /** 解析并校验 refresh token，返回 userId；非法/过期/类型错误抛 TOKEN_INVALID。 */
    public Long parseUserId(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
            if (!"refresh".equals(c.get("typ", String.class))) {
                throw new BusinessException(ResultCode.TOKEN_INVALID);
            }
            return Long.valueOf(c.getSubject());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
    }
}
