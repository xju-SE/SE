package com.xju.sem.module.social.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信出参（P? 消息中心会话历史行）。
 */
@Data
public class MessageDTO {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    /** 是否已读（对前端以布尔值呈现，与 entity 的 0/1 存储不同）。 */
    private Boolean isRead;
    private LocalDateTime createdAt;
}
