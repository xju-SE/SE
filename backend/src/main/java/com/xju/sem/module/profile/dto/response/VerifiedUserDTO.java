package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 已认证用户候选（跨模块契约）：{@code ProfileQueryService.listVerifiedUsersByMajor(...)} 的返回元素。
 * 供 M4 求助-校友路由构建候选池、M6 时间线取同专业参考人、M7 贡献者审核筛选使用。
 *
 * <p>本 DTO 是<b>服务端到服务端</b>的候选投影，仅包含路由/审核所需的最小字段；调用方按 userId
 * 投递通知或做进一步展示时，需自行遵守 real_name 永不对外的红线（realName 仅供授权审核场景）。
 */
@Data
@Builder
public class VerifiedUserDTO {

    private Long userId;

    /** STUDENT / ALUMNI。 */
    private String role;

    private Long majorTagId;

    private String college;

    /** 学生的年级档（1..10）；校友为 null。 */
    private Integer gradeLevel;

    /** 强隐私，仅授权审核场景使用，禁止二次公开曝光。 */
    private String realName;

    private String avatarUrl;
}
