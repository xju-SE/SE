package com.xju.sem.module.social.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表行（消息中心首页，一行=一个对端 + 最后一条消息摘要 + 未读数）。
 */
@Data
public class ConversationDTO {

    /** 对端 user.id。 */
    private Long peerId;

    /** 对端展示名，本期不查用户表，恒为空，前端用 avatarFor/昵称兜底展示。 */
    private String peerName;

    /** 与该对端之间最后一条消息的内容。 */
    private String lastContent;

    /** 最后一条消息时间，用于按时间倒序排列会话列表。 */
    private LocalDateTime lastAt;

    /** 当前用户在该会话中的未读消息数。 */
    private long unreadCount;
}
