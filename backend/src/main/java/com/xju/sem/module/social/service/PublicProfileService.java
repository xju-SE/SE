package com.xju.sem.module.social.service;

import com.xju.sem.module.social.dto.PublicUserDTO;

/**
 * 查看他人主页（公开资料）。
 */
public interface PublicProfileService {

    /**
     * 组装 targetId 用户的公开主页资料。
     *
     * @param viewerId 当前登录用户 id，用于判断 following 状态
     * @param targetId 被查看用户 id
     */
    PublicUserDTO getPublicProfile(Long viewerId, Long targetId);
}
