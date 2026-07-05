package com.xju.sem.module.user.service;

import com.xju.sem.module.user.constant.Role;
import com.xju.sem.module.user.dto.PrivacySettingRequest;
import com.xju.sem.module.user.dto.RegisterRequest;
import com.xju.sem.module.user.dto.UpdateProfileRequest;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.dto.UserDTO;

import java.util.List;

/**
 * 用户账号服务。四张身份表的读写唯一入口，其他模块只通过本接口只读依赖，不直接查表。
 */
public interface UserService {

    /** 注册：唯一性校验 + BCrypt 加密，创建 user（auth_status=UNVERIFIED）。 */
    UserDTO register(RegisterRequest request);

    /** 按 id 聚合 user + profile 摘要。 */
    UserDTO getById(Long userId);

    /** 当前登录用户信息。 */
    UserDTO getCurrentUser();

    /** 跨模块摘要（M7 审核列表 / 担保候选 / M4/M5 展示）。 */
    UserBriefDTO getBrief(Long userId);

    /** 修改展示型基本信息（头像/简介，落在 profile 上）。 */
    void updateProfile(Long userId, UpdateProfileRequest request);

    /** 修改隐私可见范围。 */
    void updatePrivacySetting(Long userId, PrivacySettingRequest request);

    /** 是否已通过身份认证（M4/M5 发布前置校验）。 */
    boolean isVerified(Long userId);

    /** 注册身份类型（M5 判定校友发布权限）。 */
    Role getRole(Long userId);

    /** 查询同专业且已认证的可担保候选人。 */
    List<UserBriefDTO> searchGuarantorCandidates(String major, String keyword);

    void disableUser(Long userId, String reason);

    void enableUser(Long userId);
}
