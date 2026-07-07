package com.xju.sem.module.social.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.social.dto.response.FollowStatusDTO;
import com.xju.sem.module.social.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户关注功能。当前登录用户对 {id} 用户的关注/取消关注/关注状态查询。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("@authGuard.isLogin()")
public class FollowController {

    private final FollowService followService;

    /** 关注 id 用户。 */
    @PostMapping("/users/{id}/follow")
    public Result<Void> follow(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        followService.follow(userId, id);
        return Result.ok();
    }

    /** 取消关注 id 用户。 */
    @DeleteMapping("/users/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        followService.unfollow(userId, id);
        return Result.ok();
    }

    /** 当前登录用户对 id 用户的关注状态 + id 用户的粉丝数/关注数。 */
    @GetMapping("/users/{id}/follow-status")
    public Result<FollowStatusDTO> followStatus(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        FollowStatusDTO dto = new FollowStatusDTO();
        dto.setFollowing(followService.isFollowing(userId, id));
        dto.setFollowerCount(followService.countFollowers(id));
        dto.setFollowingCount(followService.countFollowing(id));
        return Result.ok(dto);
    }
}
