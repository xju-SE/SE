package com.xju.sem.module.help.mapper;

import lombok.Data;

/**
 * 路由候选人只读投影（§6.2 候选池构建）。
 *
 * <p><b>跨表只读说明</b>：本投影由 {@link HelpRouteMapper} 直连 user + student_profile/alumni_profile
 * 三张他模块表 JOIN 查得——这是本期为打通"系统灵魂"闭环的临时做法，已在任务书中明确授权。
 * 未来应由 M2 暴露 {@code listVerifiedUsersByMajor(majorTagId)} 服务接口提供，届时删除本模块内的
 * 跨表 SQL，改走 Service 契约（见实现说明"假设与简化 / 未来演进"）。
 */
@Data
public class CandidateRow {

    /** 候选人 user.id。 */
    private Long userId;

    /** 注册身份 STUDENT/ALUMNI/ADMIN。 */
    private String role;

    /** 专业标签 id（ADMIN 兜底候选可能为 null）。 */
    private Long majorTagId;

    /** 年级档（仅 STUDENT 有值，ALUMNI/ADMIN 为 null）。 */
    private Integer gradeLevel;
}
