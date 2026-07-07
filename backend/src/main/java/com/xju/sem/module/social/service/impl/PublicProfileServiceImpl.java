package com.xju.sem.module.social.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.social.dto.PublicUserDTO;
import com.xju.sem.module.social.mapper.ProfileViewMapper;
import com.xju.sem.module.social.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicProfileServiceImpl implements PublicProfileService {

    private final ProfileViewMapper profileViewMapper;

    @Override
    public PublicUserDTO getPublicProfile(Long viewerId, Long targetId) {
        if (targetId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        PublicUserDTO dto = profileViewMapper.baseInfo(targetId);
        if (dto == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        dto.setTags(profileViewMapper.tags(targetId));
        dto.setBadges(profileViewMapper.badges(targetId));
        dto.setFollowerCount(profileViewMapper.countFollowers(targetId));
        dto.setFollowingCount(profileViewMapper.countFollowing(targetId));
        dto.setFollowing(viewerId != null && profileViewMapper.isFollowing(viewerId, targetId));
        dto.setPostCount(profileViewMapper.postCount(targetId));
        return dto;
    }
}
