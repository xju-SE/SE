package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 聚合视图中按学期分组的一组节点（大一上→大四下自然序）。 */
@Data
@Builder
public class StageGroupDTO {

    /** GRADE1_1..GRADE4_2。 */
    private String stage;
    /** stage 中文展示名（如"大一上"）。 */
    private String stageLabel;
    private List<TimelineNodeItemDTO> nodes;
}
