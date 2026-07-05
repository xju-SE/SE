package com.xju.sem.module.profile.service;

import com.xju.sem.module.profile.dto.request.UpdateStudentProfileRequest;
import com.xju.sem.module.profile.dto.response.StudentProfileDTO;
import com.xju.sem.module.profile.dto.response.TagDTO;

import java.util.List;

/**
 * 在校生画像服务。包含<b>跨模块契约方法</b>（getProfile / listUserTags，签名与地基契约一致，
 * 供 M4/M6/M7 只读调用）与本模块 Controller 内部使用的画像编辑方法。
 */
public interface StudentProfileService {

    /** 跨模块契约：按 userId 取在校生画像（含成长标签）。画像不存在抛 40202。 */
    StudentProfileDTO getProfile(Long userId);

    /** 跨模块契约：按 userId 取成长标签（tag_type∈{INTEREST,GROWTH}）。委托 UserTagService。 */
    List<TagDTO> listUserTags(Long userId);

    /** 供 M1 判断认证后是否需引导完善画像：是否已存在在校生档案。 */
    boolean existsProfile(Long userId);

    /** FR-M2-01 编辑本人画像（GPA/目标城市行业/简介/头像；专业年级学号只读不改）。 */
    StudentProfileDTO updateProfile(Long userId, UpdateStudentProfileRequest request);
}
