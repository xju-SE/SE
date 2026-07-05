package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

/** 分学期完成度（FR-M6-11）。 */
@Data
@Builder
public class StageProgressDTO {

    /** GRADE1_1..GRADE4_2。 */
    private String stage;
    private String stageLabel;
    private int totalNodes;
    private int doneNodes;
}
