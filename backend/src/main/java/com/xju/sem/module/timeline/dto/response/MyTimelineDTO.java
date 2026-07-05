package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 我的成长时间线聚合视图（FR-M6-05 动态导航聚合）。
 *
 * <p>三种可服务性状态：
 * <ul>
 *   <li>正常：{@code routeType} 非空、{@code stages} 有内容；</li>
 *   <li>待决策：{@code needsRouteDecision=true}——已进入决策窗口（大二下及以后）但用户仍未选择
 *       分化路线，前端提示"请选择发展方向"，{@code stages} 为空；</li>
 *   <li>超出服务范围：{@code graduated=true}——已毕业/超龄，本模块仅覆盖本科四年（§6.1）。</li>
 * </ul>
 */
@Data
@Builder
public class MyTimelineDTO {

    private Long majorTagId;
    /** 当前生效路线；待决策时为 null。UNDECIDED 表示未决策通用默认线。 */
    private String routeType;
    /** 当前所处学期 GRADE1_1..GRADE4_2；毕业/超龄时为 null。 */
    private String currentStage;
    /** 是否需用户先选择分化路线（决策窗口后仍未选）。 */
    private boolean needsRouteDecision;
    /** 是否已超出本科阶段服务范围。 */
    private boolean graduated;
    /** 按学期分组的节点（自然序）；待决策/毕业时为空列表。 */
    private List<StageGroupDTO> stages;
    private OverallProgressDTO overallProgress;
}
