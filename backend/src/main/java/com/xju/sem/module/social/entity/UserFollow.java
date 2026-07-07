package com.xju.sem.module.social.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户关注关系（对应 schema.sql {@code user_follow} 表，唯一键 follower_id+followee_id）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_follow")
public class UserFollow extends BaseEntity {

    /** 关注者 user.id。 */
    private Long followerId;

    /** 被关注者 user.id。 */
    private Long followeeId;
}
