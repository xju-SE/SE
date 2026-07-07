package com.xju.sem.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.social.entity.UserFollow;
import com.xju.sem.module.social.mapper.UserFollowMapper;
import com.xju.sem.module.social.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper userFollowMapper;

    @Override
    public void follow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "followerId/followeeId 不能为空");
        }
        if (followerId.equals(followeeId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "不能关注自己");
        }
        if (userFollowMapper.existsFollow(followerId, followeeId)) {
            // 已关注，幂等成功
            return;
        }
        UserFollow uf = new UserFollow();
        uf.setFollowerId(followerId);
        uf.setFolloweeId(followeeId);
        userFollowMapper.insert(uf);
    }

    @Override
    public void unfollow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "followerId/followeeId 不能为空");
        }
        // 逻辑删除（UserFollow 继承 BaseEntity，deleted 列 @TableLogic，wrapper 删除自动带 deleted=0 条件并置 deleted=1）；
        // 该关注关系不存在时也不报错，幂等成功。
        userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId));
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return false;
        }
        return userFollowMapper.existsFollow(followerId, followeeId);
    }

    @Override
    public long countFollowers(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        return userFollowMapper.countFollowers(userId);
    }

    @Override
    public long countFollowing(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        return userFollowMapper.countFollowing(userId);
    }
}
