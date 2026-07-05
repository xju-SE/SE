package com.xju.sem.module.profile.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.profile.dto.response.VerifiedUserDTO;
import com.xju.sem.module.profile.service.ProfileQueryService;
import com.xju.sem.module.user.constant.AuthConst;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.entity.AlumniProfile;
import com.xju.sem.module.user.entity.StudentProfile;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 画像跨模块查询实现。候选池 = 按专业查档案（先过 major/college/grade 硬过滤）→ 逐个经
 * {@link UserService#getBrief} 复核认证态（VERIFIED）→ 组装 {@link VerifiedUserDTO}。
 *
 * <p><b>认证态过滤说明</b>：本期以 auth_status=VERIFIED 为候选门槛（对齐 M4 路由"须已认证"口径）；
 * account status=DISABLED 的极端情况暂由调用方或后续 UserService 扩展补充（M1 未暴露 isActive 契约），
 * 见实现说明"假设与简化"。
 */
@Service
@RequiredArgsConstructor
public class ProfileQueryServiceImpl implements ProfileQueryService {

    private final IdentityProfileSupport identitySupport;
    private final UserService userService;

    @Override
    public List<VerifiedUserDTO> listVerifiedUsersByMajor(String role, Long majorTagId,
                                                          String college, Integer minGradeLevel) {
        if (majorTagId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "majorTagId 必填");
        }
        boolean wantStudent = role == null || AuthConst.RoleName.STUDENT.equals(role);
        boolean wantAlumni = role == null || AuthConst.RoleName.ALUMNI.equals(role);

        List<VerifiedUserDTO> result = new ArrayList<>();

        if (wantAlumni) {
            for (AlumniProfile p : identitySupport.listAlumniProfilesByMajor(majorTagId, college)) {
                UserBriefDTO ub = safeVerifiedBrief(p.getUserId());
                if (ub != null) {
                    result.add(VerifiedUserDTO.builder()
                            .userId(p.getUserId())
                            .role(AuthConst.RoleName.ALUMNI)
                            .majorTagId(p.getMajorTagId())
                            .college(p.getCollege())
                            .gradeLevel(null)
                            .realName(p.getRealName())
                            .avatarUrl(p.getAvatarUrl())
                            .build());
                }
            }
        }
        if (wantStudent) {
            for (StudentProfile p : identitySupport.listStudentProfilesByMajor(majorTagId, college, minGradeLevel)) {
                UserBriefDTO ub = safeVerifiedBrief(p.getUserId());
                if (ub != null) {
                    result.add(VerifiedUserDTO.builder()
                            .userId(p.getUserId())
                            .role(AuthConst.RoleName.STUDENT)
                            .majorTagId(p.getMajorTagId())
                            .college(p.getCollege())
                            .gradeLevel(p.getGradeLevel())
                            .realName(p.getRealName())
                            .avatarUrl(p.getAvatarUrl())
                            .build());
                }
            }
        }
        return result;
    }

    /** 取摘要并要求 auth_status=VERIFIED，否则返回 null（跨模块只走 UserService 契约）。 */
    private UserBriefDTO safeVerifiedBrief(Long userId) {
        UserBriefDTO ub = userService.getBrief(userId);
        if (ub != null && AuthConst.AuthStatus.VERIFIED.equals(ub.getAuthStatus())) {
            return ub;
        }
        return null;
    }
}
