package com.xju.sem.module.social.dto.response;

import lombok.Data;

/**
 * 关注状态出参：当前登录用户对目标用户的关注态 + 目标用户的粉丝/关注数。
 */
@Data
public class FollowStatusDTO {

    /** 当前登录用户是否已关注目标用户。 */
    private boolean following;

    /** 目标用户的粉丝数（被多少人关注）。 */
    private long followerCount;

    /** 目标用户关注了多少人。 */
    private long followingCount;
}
