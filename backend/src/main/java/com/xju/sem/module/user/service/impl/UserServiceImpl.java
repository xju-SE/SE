package com.xju.sem.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.user.constant.AuthConst;
import com.xju.sem.module.user.constant.Role;
import com.xju.sem.module.user.dto.PrivacySettingRequest;
import com.xju.sem.module.user.dto.RegisterRequest;
import com.xju.sem.module.user.dto.UpdateProfileRequest;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.dto.UserDTO;
import com.xju.sem.module.user.entity.AlumniProfile;
import com.xju.sem.module.user.entity.StudentProfile;
import com.xju.sem.module.user.entity.User;
import com.xju.sem.module.user.mapper.AlumniProfileMapper;
import com.xju.sem.module.user.mapper.StudentProfileMapper;
import com.xju.sem.module.user.mapper.UserMapper;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户账号服务实现。四张身份表的读写唯一入口。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final AlumniProfileMapper alumniProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final MajorTagResolver majorTagResolver;

    @Override
    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "两次输入的密码不一致");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE, "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getIdentityType());
        user.setAuthStatus(AuthConst.AuthStatus.UNVERIFIED);
        user.setStatus(AuthConst.UserStatus.ACTIVE);
        user.setContactVisibility(AuthConst.Visibility.SELF);
        user.setProfileVisibility(AuthConst.Visibility.SAME_MAJOR);
        userMapper.insert(user);
        // profile 记录待身份认证通过时才创建（schema 的 profile 关键身份列非空，不建空占位）
        return buildUserDTO(user);
    }

    @Override
    public UserDTO getById(Long userId) {
        User user = requireUser(userId);
        return buildUserDTO(user);
    }

    @Override
    public UserDTO getCurrentUser() {
        return getById(SecurityUtil.currentUserId());
    }

    @Override
    public UserBriefDTO getBrief(Long userId) {
        User user = requireUser(userId);
        UserBriefDTO dto = new UserBriefDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setAuthStatus(user.getAuthStatus());
        if (AuthConst.RoleName.STUDENT.equals(user.getRole())) {
            StudentProfile p = findStudentProfile(userId);
            if (p != null) {
                dto.setRealName(p.getRealName());
                dto.setMajorTagId(p.getMajorTagId());
                dto.setAvatarUrl(p.getAvatarUrl());
            }
        } else if (AuthConst.RoleName.ALUMNI.equals(user.getRole())) {
            AlumniProfile p = findAlumniProfile(userId);
            if (p != null) {
                dto.setRealName(p.getRealName());
                dto.setMajorTagId(p.getMajorTagId());
                dto.setAvatarUrl(p.getAvatarUrl());
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        if (AuthConst.RoleName.STUDENT.equals(user.getRole())) {
            StudentProfile p = requireStudentProfile(userId);
            if (request.getAvatarUrl() != null) {
                p.setAvatarUrl(request.getAvatarUrl());
            }
            if (request.getBio() != null) {
                p.setBio(request.getBio());
            }
            studentProfileMapper.updateById(p);
        } else if (AuthConst.RoleName.ALUMNI.equals(user.getRole())) {
            AlumniProfile p = requireAlumniProfile(userId);
            if (request.getAvatarUrl() != null) {
                p.setAvatarUrl(request.getAvatarUrl());
            }
            if (request.getBio() != null) {
                p.setBio(request.getBio());
            }
            alumniProfileMapper.updateById(p);
        } else {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该账号无可编辑档案");
        }
    }

    @Override
    @Transactional
    public void updatePrivacySetting(Long userId, PrivacySettingRequest request) {
        User user = requireUser(userId);
        if (request.getContactVisibility() != null) {
            if (!AuthConst.Visibility.isValid(request.getContactVisibility())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "contactVisibility 取值非法");
            }
            user.setContactVisibility(request.getContactVisibility());
        }
        if (request.getProfileVisibility() != null) {
            if (!AuthConst.Visibility.isValid(request.getProfileVisibility())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "profileVisibility 取值非法");
            }
            user.setProfileVisibility(request.getProfileVisibility());
        }
        userMapper.updateById(user);
    }

    @Override
    public boolean isVerified(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && AuthConst.AuthStatus.VERIFIED.equals(user.getAuthStatus());
    }

    @Override
    public Role getRole(Long userId) {
        return Role.of(requireUser(userId).getRole());
    }

    @Override
    public List<UserBriefDTO> searchGuarantorCandidates(String major, String keyword) {
        Long majorTagId = majorTagResolver.resolve(major);
        Set<Long> candidateIds = new LinkedHashSet<>();

        LambdaQueryWrapper<AlumniProfile> aw = new LambdaQueryWrapper<AlumniProfile>()
                .eq(AlumniProfile::getMajorTagId, majorTagId);
        if (StringUtils.hasText(keyword)) {
            aw.like(AlumniProfile::getRealName, keyword);
        }
        aw.last("limit 50");
        for (AlumniProfile p : alumniProfileMapper.selectList(aw)) {
            candidateIds.add(p.getUserId());
        }

        LambdaQueryWrapper<StudentProfile> sw = new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getMajorTagId, majorTagId);
        if (StringUtils.hasText(keyword)) {
            sw.like(StudentProfile::getRealName, keyword);
        }
        sw.last("limit 50");
        for (StudentProfile p : studentProfileMapper.selectList(sw)) {
            candidateIds.add(p.getUserId());
        }

        List<UserBriefDTO> result = new ArrayList<>();
        for (Long uid : candidateIds) {
            User u = userMapper.selectById(uid);
            if (u != null
                    && AuthConst.AuthStatus.VERIFIED.equals(u.getAuthStatus())
                    && AuthConst.UserStatus.ACTIVE.equals(u.getStatus())) {
                result.add(getBrief(uid));
                if (result.size() >= 20) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void disableUser(Long userId, String reason) {
        User user = requireUser(userId);
        user.setStatus(AuthConst.UserStatus.DISABLED);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void enableUser(Long userId) {
        User user = requireUser(userId);
        user.setStatus(AuthConst.UserStatus.ACTIVE);
        userMapper.updateById(user);
    }

    // ---------------- 内部辅助 ----------------

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private StudentProfile findStudentProfile(Long userId) {
        return studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, userId));
    }

    private AlumniProfile findAlumniProfile(Long userId) {
        return alumniProfileMapper.selectOne(new LambdaQueryWrapper<AlumniProfile>()
                .eq(AlumniProfile::getUserId, userId));
    }

    private StudentProfile requireStudentProfile(Long userId) {
        StudentProfile p = findStudentProfile(userId);
        if (p == null) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "请先完成身份认证再编辑资料");
        }
        return p;
    }

    private AlumniProfile requireAlumniProfile(Long userId) {
        AlumniProfile p = findAlumniProfile(userId);
        if (p == null) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "请先完成身份认证再编辑资料");
        }
        return p;
    }

    private UserDTO buildUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setAuthStatus(user.getAuthStatus());
        dto.setStatus(user.getStatus());
        dto.setContactVisibility(user.getContactVisibility());
        dto.setProfileVisibility(user.getProfileVisibility());
        if (AuthConst.RoleName.STUDENT.equals(user.getRole())) {
            StudentProfile p = findStudentProfile(user.getId());
            if (p != null) {
                dto.setRealName(p.getRealName());
                dto.setCollege(p.getCollege());
                dto.setMajorTagId(p.getMajorTagId());
                dto.setAvatarUrl(p.getAvatarUrl());
                dto.setBio(p.getBio());
                dto.setStudentNo(p.getStudentNo());
                dto.setEnrollYear(p.getEnrollYear());
                dto.setGradeLevel(p.getGradeLevel());
                dto.setGpa(p.getGpa());
                dto.setGpaScale(p.getGpaScale());
            }
        } else if (AuthConst.RoleName.ALUMNI.equals(user.getRole())) {
            AlumniProfile p = findAlumniProfile(user.getId());
            if (p != null) {
                dto.setRealName(p.getRealName());
                dto.setCollege(p.getCollege());
                dto.setMajorTagId(p.getMajorTagId());
                dto.setAvatarUrl(p.getAvatarUrl());
                dto.setBio(p.getBio());
                dto.setGradYear(p.getGradYear());
                dto.setDegreeType(p.getDegreeType());
                dto.setIsContributorBadge(p.getIsContributorBadge());
                dto.setHelpedCount(p.getHelpedCount());
                dto.setAdoptedCount(p.getAdoptedCount());
            }
        }
        return dto;
    }
}
