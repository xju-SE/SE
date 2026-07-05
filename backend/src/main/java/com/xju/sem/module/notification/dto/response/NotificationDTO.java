package com.xju.sem.module.notification.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知出参（P17 通知中心列表行）。
 */
@Data
public class NotificationDTO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String refType;
    private Long refId;
    /** 是否已读 0/1（与 entity 存储一致，前端按 0/1 判定）。 */
    private Integer isRead;
    private String channel;
    private LocalDateTime createdAt;
}
