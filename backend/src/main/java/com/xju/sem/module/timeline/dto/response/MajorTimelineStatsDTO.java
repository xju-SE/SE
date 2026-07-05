package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 专业级时间线完成度统计（FR-M6-12，供 M7/首页仪表盘运营视角）：聚合某 major×route 已发布模板下
 * 全部用户的节点完成率分布。{@code templateId} 为实际解析出的模板（可能是专业专属或通用兜底）。
 */
@Data
@Builder
public class MajorTimelineStatsDTO {

    private Long majorTagId;
    private String routeType;
    private Long templateId;
    /** 模板下节点总数。 */
    private int totalNodes;
    /** 有过任意进度记录的用户数（分母）。 */
    private int userCount;
    /** 全体用户平均完成率（整数 0-100）。 */
    private int avgCompletion;
    /** 完成率分布桶：完成率 0%。 */
    private int bucketZero;
    /** 完成率 (0,50%]。 */
    private int bucketLow;
    /** 完成率 (50%,100%)。 */
    private int bucketMid;
    /** 完成率 100%。 */
    private int bucketFull;
}
