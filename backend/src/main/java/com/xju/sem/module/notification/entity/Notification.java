package com.xju.sem.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内通知（对应 schema.sql {@code notification} 表，全局表，见地基实体清单 #25）。
 *
 * <p>本实体是全局 {@code NotificationService.send(...)} 契约唯一落库的数据结构，由 M1/M3/M4/M5/M6/M7
 * 等多方模块共同生产（各自只调用 {@link com.xju.sem.module.notification.service.NotificationService}
 * 接口写入，不直接操作本表），呈现/已读态维护统一收在本模块（P17 通知中心）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {

    /** 接收人 user.id。 */
    private Long userId;

    /** 通知类型，取值域 {@link com.xju.sem.module.notification.enums.NotificationType}：HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM。 */
    private String type;

    /** 标题，VARCHAR(100)，超长由 Service 层截断。 */
    private String title;

    /** 正文，VARCHAR(500)，超长由 Service 层截断。 */
    private String content;

    /** 关联对象类型，可空（如 HELP_TICKET/HELP_ANSWER/AUTH_APPLICATION/KNOWLEDGE_ENTRY）；跨表引用不建数据库级 FK。 */
    private String refType;

    /** 关联对象主键，可空，与 refType 配合供前端跳转定位。 */
    private Long refId;

    /** 是否已读 0/1，默认 0。 */
    private Integer isRead;

    /** 投递渠道，本期恒为 INAPP（PUSH 本期预留不投递，见 schema 列注释）。 */
    private String channel;
}
