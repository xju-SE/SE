package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 补救优先级提示（§6.5）。对已逾期未完成节点按"逾期分档 × 节点重要度 + 学期邻近度加成"打分排序，
 * 供 P16 补救提示区与首页仪表盘摘要卡消费。也用作 {@code TimelineSummaryCardDTO.topRemediationHint}。
 */
@Data
@Builder
public class RemediationHintDTO {

    private TimelineNodeDTO node;
    /** 建议完成截止日（建议月月末）。 */
    private LocalDate suggestedDate;
    private int monthsOverdue;
    private long daysOverdue;
    /** URGENT/HIGH/MEDIUM/LOW（逾期紧迫度分档）。 */
    private String priorityTier;
    /** 综合优先级分值（越大越紧迫）。 */
    private int priorityScore;
}
