package com.xju.sem.module.profile.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.profile.dto.request.UpdateStudentProfileRequest;
import com.xju.sem.module.profile.dto.response.StudentProfileDTO;
import com.xju.sem.module.profile.dto.response.TagDTO;
import com.xju.sem.module.profile.enums.TagType;
import com.xju.sem.module.profile.service.StudentProfileService;
import com.xju.sem.module.profile.service.UserTagService;
import com.xju.sem.module.user.entity.StudentProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 在校生画像服务实现（FR-M2-01）。专业/年级/学号为认证结果只读，本服务不改；仅维护成长扩展字段。
 */
@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

    private final IdentityProfileSupport identitySupport;
    private final ProfileTagSupport tagSupport;
    private final UserTagService userTagService;

    @Override
    public StudentProfileDTO getProfile(Long userId) {
        StudentProfile p = identitySupport.requireStudentProfile(userId);
        return toDTO(p);
    }

    @Override
    public List<TagDTO> listUserTags(Long userId) {
        return userTagService.listUserTags(userId);
    }

    @Override
    public boolean existsProfile(Long userId) {
        return identitySupport.findStudentProfile(userId) != null;
    }

    @Override
    @Transactional
    public StudentProfileDTO updateProfile(Long userId, UpdateStudentProfileRequest request) {
        StudentProfile p = identitySupport.requireStudentProfile(userId);

        // gpaScale 校验（仅 4/5）
        Integer scale = request.getGpaScale() != null ? request.getGpaScale() : p.getGpaScale();
        if (scale != null && scale != 4 && scale != 5) {
            throw new BusinessException(20201, "GPA 满分制仅支持 4 或 5");
        }
        // gpa ∈ [0, scale]（20201）
        if (request.getGpa() != null) {
            BigDecimal gpa = request.getGpa();
            if (gpa.compareTo(BigDecimal.ZERO) < 0
                    || (scale != null && gpa.compareTo(BigDecimal.valueOf(scale)) > 0)) {
                throw new BusinessException(20201, "GPA 超出 [0, " + scale + "] 范围");
            }
            p.setGpa(gpa);
        }
        if (request.getGpaScale() != null) {
            p.setGpaScale(request.getGpaScale());
        }
        // 目标行业须为 INDUSTRY（20201）
        if (request.getTargetIndustryTagId() != null) {
            tagSupport.requireType(request.getTargetIndustryTagId(), TagType.INDUSTRY.name());
            p.setTargetIndustryTagId(request.getTargetIndustryTagId());
        }
        if (request.getTargetCity() != null) {
            p.setTargetCity(request.getTargetCity());
        }
        if (request.getBio() != null) {
            p.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            p.setAvatarUrl(request.getAvatarUrl());
        }
        identitySupport.updateStudentProfile(p);
        return toDTO(p);
    }

    private StudentProfileDTO toDTO(StudentProfile p) {
        return StudentProfileDTO.builder()
                .userId(p.getUserId())
                .realName(p.getRealName())
                .studentNo(p.getStudentNo())
                .college(p.getCollege())
                .majorTagId(p.getMajorTagId())
                .enrollYear(p.getEnrollYear())
                .gradeLevel(p.getGradeLevel())
                .gpa(p.getGpa())
                .gpaScale(p.getGpaScale())
                .targetCity(p.getTargetCity())
                .targetIndustryTagId(p.getTargetIndustryTagId())
                .bio(p.getBio())
                .avatarUrl(p.getAvatarUrl())
                .tags(userTagService.listUserTags(p.getUserId()))
                .build();
    }
}
