package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

/** 选择/切换发展路线结果（FR-M6-07）：目标路线 + 本次批量初始化（INSERT IGNORE）覆盖的节点数。 */
@Data
@Builder
public class RouteConfirmResultDTO {

    private String routeType;
    /** 该路线模板下参与初始化的节点数（已存在进度的节点保留原状，计入此数以反映路线规模）。 */
    private int initializedNodeCount;
}
