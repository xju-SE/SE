package com.xju.sem.module.profile.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.profile.dto.request.UpdateAlumniProfileRequest;
import com.xju.sem.module.profile.dto.response.AlumniBriefDTO;
import com.xju.sem.module.profile.dto.response.AlumniProfileDTO;
import com.xju.sem.module.profile.enums.TagType;
import com.xju.sem.module.profile.service.AlumniProfileService;
import com.xju.sem.module.profile.service.UserTagService;
import com.xju.sem.module.user.constant.AuthConst;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.entity.AlumniProfile;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 毕业生档案服务实现。徽章/计数为系统写入字段（M7 审核事件 / M4 采纳事件驱动），本服务对外
 * 暴露契约写入口；/me 编辑仅动展示字段。跨模块摘要 getBrief 以登录名作句柄，不透出 real_name。
 */
@Service
@RequiredArgsConstructor
public class AlumniProfileServiceImpl implements AlumniProfileService {

    private final IdentityProfileSupport identitySupport;
    private final ProfileTagSupport tagSupport;
    private final UserTagService userTagService;
    private final UserService userService;

    @Override
    public AlumniBriefDTO getBrief(Long userId) {
        AlumniProfile p = identitySupport.requireAlumniProfile(userId);
        UserBriefDTO ub = userService.getBrief(userId);
        return AlumniBriefDTO.builder()
                .userId(userId)
                .nickname(ub == null ? null : ub.getUsername())
                .majorTagId(p.getMajorTagId())
                .college(p.getCollege())
                .gradYear(p.getGradYear())
                .degreeType(p.getDegreeType())
                .isContributorBadge(p.getIsContributorBadge())
                .helpedCount(p.getHelpedCount())
                .adoptedCount(p.getAdoptedCount())
                .avatarUrl(p.getAvatarUrl())
                .build();
    }

    @Override
    @Transactional
    public void grantContributorBadge(Long userId, String honorCertUrl) {
        identitySupport.grantContributorBadge(userId, honorCertUrl);
    }

    @Override
    @Transactional
    public void incrementHelpedCount(Long userId) {
        identitySupport.incrementHelpedCount(userId);
    }

    @Override
    @Transactional
    public void incrementAdoptedCount(Long userId) {
        identitySupport.incrementAdoptedCount(userId);
    }

    @Override
    public AlumniProfileDTO getMyProfile(Long userId) {
        AlumniProfile p = identitySupport.requireAlumniProfile(userId);
        return toDTO(p);
    }

    @Override
    @Transactional
    public AlumniProfileDTO updateMyProfile(Long userId, UpdateAlumniProfileRequest request) {
        AlumniProfile p = identitySupport.requireAlumniProfile(userId);
        if (request.getDegreeType() != null) {
            if (!isValidDegree(request.getDegreeType())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "学历层次非法");
            }
            p.setDegreeType(request.getDegreeType());
        }
        if (request.getMajorTagId() != null) {
            tagSupport.requireType(request.getMajorTagId(), TagType.MAJOR.name());
            p.setMajorTagId(request.getMajorTagId());
        }
        if (request.getCollege() != null) {
            p.setCollege(request.getCollege());
        }
        if (request.getGradYear() != null) {
            p.setGradYear(request.getGradYear());
        }
        if (request.getBio() != null) {
            p.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            p.setAvatarUrl(request.getAvatarUrl());
        }
        identitySupport.updateAlumniProfile(p);
        return toDTO(p);
    }

    private boolean isValidDegree(String v) {
        return AuthConst.DegreeType.BACHELOR.equals(v)
                || AuthConst.DegreeType.MASTER.equals(v)
                || AuthConst.DegreeType.PHD.equals(v);
    }

    private AlumniProfileDTO toDTO(AlumniProfile p) {
        return AlumniProfileDTO.builder()
                .userId(p.getUserId())
                .realName(p.getRealName())
                .college(p.getCollege())
                .majorTagId(p.getMajorTagId())
                .gradYear(p.getGradYear())
                .degreeType(p.getDegreeType())
                .isContributorBadge(p.getIsContributorBadge())
                .helpedCount(p.getHelpedCount())
                .adoptedCount(p.getAdoptedCount())
                .honorCertUrl(p.getHonorCertUrl())
                .bio(p.getBio())
                .avatarUrl(p.getAvatarUrl())
                .tags(userTagService.listUserTags(p.getUserId()))
                .build();
    }
}
