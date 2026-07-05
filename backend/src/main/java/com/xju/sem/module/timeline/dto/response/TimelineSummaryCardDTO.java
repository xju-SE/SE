package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

/** 首页仪表盘"我的成长时间线"摘要卡（供双圈首页调用）：当前路线、总体完成率、最紧迫补救提示。 */
@Data
@Builder
public class TimelineSummaryCardDTO {

    /** 当前生效路线；待决策时为 null。 */
    private String routeType;
    /** 是否需先选择分化路线。 */
    private boolean needsRouteDecision;
    /** 是否已超出本科阶段服务范围。 */
    private boolean graduated;
    /** 总体完成百分比（整数 0-100）。 */
    private int overallPercentage;
    /** 最紧迫的一条补救提示（无逾期时为 null）。 */
    private RemediationHintDTO topRemediationHint;
}
