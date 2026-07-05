package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 时间线节点出参。用于 ADMIN 节点 CRUD 回显、学生端路线预览（不含个人进度），并作为聚合视图
 * {@code TimelineNodeItemDTO.node} 的内嵌节点定义。{@code stageLabel} 为 stage 的中文展示名。
 */
@Data
@Builder
public class TimelineNodeDTO {

    private Long id;
    private Long templateId;
    private String stage;
    /** stage 中文展示名（如"大一上"）。 */
    private String stageLabel;
    private String title;
    private String description;
    /** 建议完成时间展示串（如"大一上第 8 周"）。 */
    private String suggestedTime;
    /** 建议完成月份 1-12（供逾期比对）。 */
    private Integer suggestedMonth;
    /** 重要度 1-3（越大越关键）。 */
    private Integer importance;
    private Integer orderNo;
}
