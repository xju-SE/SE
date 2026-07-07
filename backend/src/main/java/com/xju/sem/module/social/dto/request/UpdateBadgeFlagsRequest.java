package com.xju.sem.module.social.dto.request;

import lombok.Data;

/**
 * 更新徽章置顶/隐藏状态入参。字段均可空——不传表示不修改该字段，由 Service 层与既有值合并。
 */
@Data
public class UpdateBadgeFlagsRequest {

    private Boolean pinned;

    private Boolean hidden;
}
