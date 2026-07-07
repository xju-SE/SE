package com.xju.sem.module.social.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内私信（对应 schema.sql {@code message} 表）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class Message extends BaseEntity {

    /** 发送者 user.id。 */
    private Long senderId;

    /** 接收者 user.id。 */
    private Long receiverId;

    /** 消息正文，VARCHAR(2000)。 */
    private String content;

    /** 接收者是否已读 0/1，默认 0。 */
    private Integer isRead;
}
