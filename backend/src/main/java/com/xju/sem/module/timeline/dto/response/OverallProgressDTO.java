package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

/** 总体完成度（§6.4）：总节点数、已完成数、完成百分比（整数 0-100）。 */
@Data
@Builder
public class OverallProgressDTO {

    private int totalNodes;
    private int doneNodes;
    /** 完成百分比（整数 0-100）；totalNodes=0 时为 0。 */
    private int percentage;
}
