package com.xju.sem.module.social.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户徽章/成就（对应 schema.sql {@code user_badge} 表）。同一用户同一 badgeCode 一般只授予一次，
 * 由授予方（各业务模块）负责去重，本表不建唯一约束。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_badge")
public class UserBadge extends BaseEntity {

    /** 持有人 user.id。 */
    private Long userId;

    /** 徽章编码（如 FIRST_ADOPT/HELP_10），业务侧唯一标识。 */
    private String badgeCode;

    /** 徽章展示名称。 */
    private String badgeName;

    /** 徽章图标（URL 或图标标识）。 */
    private String icon;

    /** 是否置顶展示 0/1，默认 0。 */
    private Integer pinned;

    /** 是否隐藏（不公开展示）0/1，默认 0。 */
    private Integer hidden;

    /** 授予时间。 */
    private LocalDateTime awardedAt;
}
