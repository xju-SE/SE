package com.xju.sem.module.user.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.user.dto.LoginRequest;
import com.xju.sem.module.user.dto.LoginResponse;
import com.xju.sem.module.user.dto.RefreshRequest;
import com.xju.sem.module.user.dto.RegisterRequest;
import com.xju.sem.module.user.dto.RegisterResponse;
import com.xju.sem.module.user.dto.TokenPair;
import com.xju.sem.module.user.dto.UserDTO;
import com.xju.sem.module.user.service.AuthTokenService;
import com.xju.sem.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号认证入口：注册 / 登录 / 刷新 / 登出（前三者在 SecurityConfig 白名单内）。
 * Controller 仅做入参校验与转发，业务在 Service。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthTokenService authTokenService;

    /** 注册成功即自动登录，返回令牌对。 */
    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = userService.register(request);
        TokenPair tokens = authTokenService.issueFor(user.getUserId(), user.getRole(), user.getAuthStatus());
        return Result.ok(new RegisterResponse(user.getUserId(),
                tokens.getAccessToken(), tokens.getRefreshToken()));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authTokenService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/refresh")
    public Result<TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authTokenService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authTokenService.logout(SecurityUtil.currentUserId());
        return Result.ok();
    }
}
