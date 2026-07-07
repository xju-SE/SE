package com.xju.sem.module.social.service;

import com.xju.sem.module.social.dto.ConversationDTO;
import com.xju.sem.module.social.dto.MessageDTO;

import java.util.List;

/**
 * 站内私信服务（消息中心：会话列表 + 会话历史 + 发送 + 已读态）。
 */
public interface MessageService {

    /** 发送一条私信。 */
    MessageDTO send(Long senderId, Long receiverId, String content);

    /** 当前用户的会话列表，按最后消息时间倒序。 */
    List<ConversationDTO> conversations(Long userId);

    /** 与指定对端的会话历史（按时间正序），取出的同时把对方发来的未读消息标记为已读。 */
    List<MessageDTO> history(Long userId, Long peerId);

    /** 将指定对端发给当前用户的未读消息标记为已读。 */
    void markRead(Long userId, Long peerId);

    /** 当前用户未读消息总数（顶部角标）。 */
    long unreadCount(Long userId);
}
