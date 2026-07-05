package com.xju.sem.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.common.security.JwtUtil;
import com.xju.sem.module.user.constant.AuthConst;
import com.xju.sem.module.user.dto.LoginResponse;
import com.xju.sem.module.user.dto.TokenPair;
import com.xju.sem.module.user.entity.User;
import com.xju.sem.module.user.mapper.UserMapper;
import com.xju.sem.module.user.service.AuthTokenService;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 令牌服务实现。access token 复用地基 JwtUtil；refresh token 由 RefreshTokenProvider 独立签发。
 */
@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenProvider refreshTokenProvider;
    private final UserService userService;

    @Override
    public LoginResponse login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_CREDENTIALS);
        }
        if (!AuthConst.UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        String access = jwtUtil.generate(user.getId(), user.getRole(), user.getAuthStatus());
        String refresh = refreshTokenProvider.generate(user.getId());
        return new LoginResponse(access, refresh, userService.getById(user.getId()));
    }

    @Override
    public TokenPair issueFor(Long userId, String role, String authStatus) {
        return new TokenPair(
                jwtUtil.generate(userId, role, authStatus),
                refreshTokenProvider.generate(userId));
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        Long userId = refreshTokenProvider.parseUserId(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        if (!AuthConst.UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        String access = jwtUtil.generate(user.getId(), user.getRole(), user.getAuthStatus());
        return new TokenPair(access, refreshToken);
    }

    @Override
    public void logout(Long userId) {
        // 无状态设计：本期不维护服务端黑名单，前端丢弃 token 即可。
    }
}
