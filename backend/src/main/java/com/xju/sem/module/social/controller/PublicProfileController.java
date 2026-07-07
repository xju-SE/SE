package com.xju.sem.module.social.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.social.dto.PublicUserDTO;
import com.xju.sem.module.social.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 查看他人主页（公开资料）。使用 /{id}/public 避免与 UserController 已有的 /{id}/status 等路径冲突。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("@authGuard.isLogin()")
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    /** 查看 id 用户的公开主页资料。 */
    @GetMapping("/{id}/public")
    public Result<PublicUserDTO> getPublicProfile(@PathVariable Long id) {
        Long viewerId = SecurityUtil.currentUserId();
        return Result.ok(publicProfileService.getPublicProfile(viewerId, id));
    }
}
