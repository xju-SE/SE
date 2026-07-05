package com.xju.sem.module.profile.service;

import com.xju.sem.module.profile.dto.request.UpdateAlumniProfileRequest;
import com.xju.sem.module.profile.dto.response.AlumniBriefDTO;
import com.xju.sem.module.profile.dto.response.AlumniProfileDTO;

/**
 * 毕业生档案服务。含<b>跨模块契约方法</b>（getBrief 供展示引用、grantContributorBadge 供 M7 审核通过后写徽章、
 * increment* 供 M4 采纳事件累加缓存计数），与本模块 /me 编辑方法。
 */
public interface AlumniProfileService {

    /** 跨模块契约：毕业生弱隐私摘要（不含 real_name）。档案不存在抛 40202。 */
    AlumniBriefDTO getBrief(Long userId);

    /** 跨模块契约（供 M7 审核通过后调用）：写入贡献者徽章标识与荣誉证明地址。 */
    void grantContributorBadge(Long userId, String honorCertUrl);

    /** 供 M4 采纳事件调用：已帮助计数 +1（缓存字段，DB 侧原子自增）。 */
    void incrementHelpedCount(Long userId);

    /** 供 M4 采纳事件调用：被采纳次数 +1（缓存字段，DB 侧原子自增）。 */
    void incrementAdoptedCount(Long userId);

    /** GET /alumni-profiles/me：本人档案详情（含成长标签、徽章计数）。 */
    AlumniProfileDTO getMyProfile(Long userId);

    /** PUT /alumni-profiles/me：编辑本人档案展示字段。 */
    AlumniProfileDTO updateMyProfile(Long userId, UpdateAlumniProfileRequest request);
}
