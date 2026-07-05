package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 推荐结果中的"匹配校友"卡片（P07）。以登录名作为展示句柄，不含 real_name。
 */
@Data
@Builder
public class MatchedAlumniDTO {

    private Long userId;

    /** 展示句柄（登录名）。 */
    private String nickname;

    /** 贡献者徽章 0/1。 */
    private Integer badge;

    private Integer adoptedCount;

    private String avatarUrl;
}
