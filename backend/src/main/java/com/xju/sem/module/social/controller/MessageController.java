package com.xju.sem.module.social.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.social.dto.ConversationDTO;
import com.xju.sem.module.social.dto.MessageDTO;
import com.xju.sem.module.social.dto.request.SendMessageRequest;
import com.xju.sem.module.social.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 消息中心（站内私信）。仅操作"当前登录用户自己"的会话，不接受也不需要 userId 入参，
 * 防止越权查看/标记他人消息。
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@PreAuthorize("@authGuard.isLogin()")
public class MessageController {

    private final MessageService messageService;

    /** 会话列表，按最后消息时间倒序。 */
    @GetMapping("/conversations")
    public Result<List<ConversationDTO>> conversations() {
        Long userId = SecurityUtil.currentUserId();
        return Result.ok(messageService.conversations(userId));
    }

    /** 与指定对端的会话历史，取出的同时标记对方发来的消息为已读。 */
    @GetMapping("/conversations/{peerId}")
    public Result<List<MessageDTO>> history(@PathVariable Long peerId) {
        Long userId = SecurityUtil.currentUserId();
        return Result.ok(messageService.history(userId, peerId));
    }

    /** 发送一条私信。 */
    @PostMapping
    public Result<MessageDTO> send(@Valid @RequestBody SendMessageRequest request) {
        Long userId = SecurityUtil.currentUserId();
        return Result.ok(messageService.send(userId, request.getReceiverId(), request.getContent()));
    }

    /** 将指定对端发来的消息标记为已读。 */
    @PatchMapping("/conversations/{peerId}/read")
    public Result<Void> markRead(@PathVariable Long peerId) {
        Long userId = SecurityUtil.currentUserId();
        messageService.markRead(userId, peerId);
        return Result.ok();
    }

    /** 未读消息总数（顶部角标）。 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtil.currentUserId();
        return Result.ok(messageService.unreadCount(userId));
    }
}
