package com.xju.sem.module.social.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 徽章出参（个人主页/资料页徽章墙展示行）。
 */
@Data
public class BadgeDTO {

    private Long id;

    /** = badgeCode。 */
    private String code;

    /** = badgeName。 */
    private String name;

    private String icon;

    /** 是否置顶展示（与 entity 的 0/1 转换，便于前端布尔判定）。 */
    private Boolean pinned;

    /** 是否隐藏（不公开展示）。 */
    private Boolean hidden;

    private LocalDateTime awardedAt;
}
