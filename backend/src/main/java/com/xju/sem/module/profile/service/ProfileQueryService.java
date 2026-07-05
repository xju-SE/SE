package com.xju.sem.module.profile.service;

import com.xju.sem.module.profile.dto.response.VerifiedUserDTO;

import java.util.List;

/**
 * 画像跨模块查询服务。当前只承载一个跨模块契约方法：按专业列出已认证用户候选，
 * 供 M4 求助-校友路由构建候选池、M6 时间线取同专业参考人、M7 贡献者审核筛选。
 *
 * <p>本方法上线后，M4 现有的 {@code HelpRouteMapper} 直连 user/student_profile/alumni_profile
 * 的临时 JOIN 查询即可下线，改走本契约（见 M4 实现说明中的迁移注记）。
 */
public interface ProfileQueryService {

    /**
     * 跨模块契约：按专业查询已认证（VERIFIED）且启用（ACTIVE）的用户候选。
     *
     * @param role         角色过滤：STUDENT / ALUMNI；传 null 表示两者都要
     * @param majorTagId   专业标签 id，必填
     * @param college      学院过滤，可空（空则不按学院过滤）
     * @param minGradeLevel 学生最低年级档（含），可空；仅对 STUDENT 生效（找高年级学长学姐时用）
     */
    List<VerifiedUserDTO> listVerifiedUsersByMajor(String role, Long majorTagId,
                                                   String college, Integer minGradeLevel);
}
