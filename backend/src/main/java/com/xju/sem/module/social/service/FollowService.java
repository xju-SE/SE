package com.xju.sem.module.social.service;

/**
 * 用户关注服务（P?? 关注功能）。
 */
public interface FollowService {

    /**
     * 关注。followerId==followeeId 视为非法操作直接抛异常；已关注则幂等成功（不重复插入）。
     *
     * @param followerId 关注者 user.id
     * @param followeeId 被关注者 user.id
     */
    void follow(Long followerId, Long followeeId);

    /** 取消关注；不存在该关注关系时幂等成功。 */
    void unfollow(Long followerId, Long followeeId);

    /** followerId 是否已关注 followeeId。 */
    boolean isFollowing(Long followerId, Long followeeId);

    /** userId 的粉丝数（被多少人关注）。 */
    long countFollowers(Long userId);

    /** userId 关注了多少人。 */
    long countFollowing(Long userId);
}
