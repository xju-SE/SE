package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 毕业生摘要（跨模块契约）：{@code AlumniProfileService.getBrief(Long)} 的返回类型，
 * 供 M4 路由展示、M6 时间线引用、M7 贡献者审核列表、推荐结果的"匹配校友"卡片使用。
 *
 * <p>只暴露弱隐私摘要：以 {@code nickname}（登录名）作为展示句柄，<b>不携带 real_name</b>
 * （real_name 永不对外，见 §6.2）。徽章与计数为 alumni_profile 缓存字段的只读回显。
 */
@Data
@Builder
public class AlumniBriefDTO {

    private Long userId;

    /** 展示句柄（登录名），非真实姓名。 */
    private String nickname;

    private Long majorTagId;

    private String college;

    private Integer gradYear;

    /** BACHELOR/MASTER/PHD。 */
    private String degreeType;

    /** 贡献者认证标识 0/1。 */
    private Integer isContributorBadge;

    private Integer helpedCount;

    private Integer adoptedCount;

    private String avatarUrl;
}
