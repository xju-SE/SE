package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 聚合视图中的单个节点项（§6.4）：节点定义 + 个人进度 + 逾期标记 + 关联引用摘要。
 * {@code suggestedDate} 为按 stage+suggestedMonth+enrollYear 换算的建议截止日（当月月末）；
 * 逾期以月粒度判定（§6.1），同时给出 monthsOverdue 与 daysOverdue 供展示层选择呈现。
 */
@Data
@Builder
public class TimelineNodeItemDTO {

    private TimelineNodeDTO node;
    /** NOT_STARTED/DONE。 */
    private String progressStatus;
    /** 建议完成截止日（建议月月末）；suggestedMonth 为空时为 null。 */
    private LocalDate suggestedDate;
    private boolean overdue;
    private int monthsOverdue;
    private long daysOverdue;
    private List<TimelineNodeRefDTO> refs;
}
