package com.xju.sem.module.social.service;

import com.xju.sem.module.social.dto.BadgeDTO;

import java.util.List;

/**
 * 用户徽章/成就服务。公开列表（他人主页可见）与本人全量列表分开对外，写操作（置顶/隐藏）仅限本人。
 */
public interface BadgeService {

    /** userId 的公开徽章列表（hidden=0），置顶优先、按授予时间倒序。 */
    List<BadgeDTO> listPublic(Long userId);

    /** 当前登录用户自己的全部徽章（含隐藏）。 */
    List<BadgeDTO> listMine(Long userId);

    /**
     * 更新 badgeId 徽章的置顶/隐藏状态；仅本人可操作，非本人或不存在时抛异常。
     * pinned/hidden 任一为 null 表示不修改该字段，与数据库既有值合并后落库。
     */
    void setFlags(Long userId, Long badgeId, Boolean pinned, Boolean hidden);
}
