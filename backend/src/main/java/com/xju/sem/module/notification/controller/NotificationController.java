package com.xju.sem.module.notification.controller;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.notification.dto.response.NotificationDTO;
import com.xju.sem.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * P17 站内通知中心（全局，07 集成报告 C-D 缺口补齐）。仅做入参校验转发 + 取当前登录用户，
 * 业务逻辑在 {@link NotificationService}。全部接口只操作"当前登录用户自己"的通知，不接受
 * 也不需要 userId 入参——防止越权查看/标记他人通知。已登录（无论 authStatus 是否 VERIFIED）
 * 即可访问：认证结果通知本身就是发给尚未认证用户的，若拦到 isVerified() 会形成"收不到自己
 * 认证结果通知"的悖论，故用 {@code @authGuard.isLogin()} 而非 {@code isVerified()}。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("@authGuard.isLogin()")
public class NotificationController {

    private final NotificationService notificationService;

    /** 分页列表，可选 isRead 筛选（不传=全部），按创建时间倒序。 */
    @GetMapping
    public Result<PageResult<NotificationDTO>> list(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.currentUserId();
        return Result.ok(notificationService.pageList(userId, isRead, page, size));
    }

    /** 未读数（顶部角标）。 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtil.currentUserId();
        return Result.ok(notificationService.countUnread(userId));
    }

    /** 标记单条已读。 */
    @PatchMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        notificationService.markRead(userId, id);
        return Result.ok();
    }

    /** 全部标记已读。 */
    @PatchMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = SecurityUtil.currentUserId();
        notificationService.markAllRead(userId);
        return Result.ok();
    }
}
