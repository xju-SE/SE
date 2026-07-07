package com.xju.sem.module.social.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.social.dto.BadgeDTO;
import com.xju.sem.module.social.dto.request.UpdateBadgeFlagsRequest;
import com.xju.sem.module.social.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户徽章/成就。{id} 用户主页展示公开徽章墙，本人可查看全部（含隐藏）并调整置顶/隐藏状态。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("@authGuard.isLogin()")
public class BadgeController {

    private final BadgeService badgeService;

    /** id 用户的公开徽章列表（hidden=0），供他人主页展示。 */
    @GetMapping("/users/{id}/badges")
    public Result<List<BadgeDTO>> listPublic(@PathVariable Long id) {
        return Result.ok(badgeService.listPublic(id));
    }

    /** 当前登录用户自己的全部徽章（含隐藏）。 */
    @GetMapping("/users/me/badges")
    public Result<List<BadgeDTO>> listMine() {
        Long userId = SecurityUtil.currentUserId();
        return Result.ok(badgeService.listMine(userId));
    }

    /** 更新 id 徽章的置顶/隐藏状态；仅本人可操作，未传字段不修改。 */
    @PatchMapping("/badges/{id}")
    public Result<Void> updateFlags(@PathVariable Long id, @RequestBody UpdateBadgeFlagsRequest request) {
        Long userId = SecurityUtil.currentUserId();
        badgeService.setFlags(userId, id, request.getPinned(), request.getHidden());
        return Result.ok();
    }
}
