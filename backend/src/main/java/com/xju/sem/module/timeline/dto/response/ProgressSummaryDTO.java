package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 整体完成度统计（FR-M6-11，个人视角）：总体 + 按学期分组。 */
@Data
@Builder
public class ProgressSummaryDTO {

    private OverallProgressDTO overallProgress;
    private List<StageProgressDTO> byStage;
}
