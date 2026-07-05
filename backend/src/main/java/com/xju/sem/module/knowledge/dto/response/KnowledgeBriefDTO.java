package com.xju.sem.module.knowledge.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 知识条目摘要出参。同时承担两个角色：
 * 1) 本模块 list/search/mine 列表页的行数据（P08 卡片）；
 * 2) 跨模块契约 {@code KnowledgeEntryService.getBrief(Long id)} 的返回类型（供 M6/M7 只读引用），
 *    类名 KnowledgeBriefDTO 与地基契约签名保持一致，不另建重名 DTO。
 */
@Data
@Builder
public class KnowledgeBriefDTO {

    private Long id;
    private String title;
    private String category;
    private String status;
    private String sourceType;
    private Long authorId;
    private Long claimerId;
    private String applicableScope;
    private LocalDate validUntil;
    private String externalUrl;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 仅 search 结果携带相关度；list/mine/getBrief 场景为 null。 */
    private Double relevance;
}
