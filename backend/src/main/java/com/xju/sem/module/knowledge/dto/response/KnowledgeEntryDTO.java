package com.xju.sem.module.knowledge.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 知识条目详情出参（GET /{id}、create/update/submit/claim/offline 的返回体）。 */
@Data
@Builder
public class KnowledgeEntryDTO {

    private Long id;
    private String title;
    private String content;
    private String category;
    private Long authorId;
    private Long claimerId;
    private String applicableScope;
    private LocalDate validUntil;
    private String externalUrl;
    private String status;
    private String sourceType;
    private Long sourceHelpId;
    private Integer viewCount;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 供前端直接控制按钮显隐：当前登录用户是否可编辑（作者/认领人/ADMIN 且非 REVIEWING）。 */
    private boolean editable;

    /** 当前登录用户是否可认领（状态在 PUBLISHED/EXPIRED/OFFLINE 且未被他人认领）。 */
    private boolean claimable;

    /** 当前登录用户是否可删除（作者本人且非 PUBLISHED，或 ADMIN）。 */
    private boolean deletable;
}
