package com.xju.sem.module.timeline.service;

import com.xju.sem.module.timeline.dto.response.MyTimelineDTO;
import com.xju.sem.module.timeline.dto.response.ProgressSummaryDTO;
import com.xju.sem.module.timeline.dto.response.RemediationHintDTO;
import com.xju.sem.module.timeline.dto.response.RouteConfirmResultDTO;
import com.xju.sem.module.timeline.dto.response.TimelineNodeDTO;
import com.xju.sem.module.timeline.dto.response.TimelineSummaryCardDTO;
import com.xju.sem.module.timeline.dto.response.UserProgressDTO;

import java.util.List;

/**
 * 个人进度与动态导航服务（STUDENT）。承载本模块核心差异化能力：§6.3 路线解析/切换、§6.4 逾期
 * 比对、§6.5 补救打分、§6.6 懒初始化。所有写路径均在 Service 层做越权防护——校验目标节点确属该
 * 用户当前 {@code resolveEffectiveRouteAndTemplate} 解出的有效模板（§9）。
 */
public interface UserProgressService {

    /** FR-M6-05 查看我的成长时间线（动态导航聚合视图）：解析有效模板→懒初始化→逐节点逾期比对→聚合。 */
    MyTimelineDTO getMyTimeline(Long userId);

    /** 首页仪表盘摘要卡：当前路线 + 总体完成率 + 最紧迫补救提示。 */
    TimelineSummaryCardDTO getMySummaryCard(Long userId);

    /** FR-M6-06 预览某路线内容（决策前对比），不写入任何个人进度。 */
    List<TimelineNodeDTO> previewRoute(Long userId, String routeType);

    /** FR-M6-07 选择/切换发展路线：为目标模板全部节点批量 INSERT IGNORE 进度。UNDECIDED 不可选入（30603）。 */
    RouteConfirmResultDTO confirmRoute(Long userId, String routeType);

    /** FR-M6-08 标记/切换节点个人进度（越权防护：节点须属当前有效模板）。 */
    UserProgressDTO markProgress(Long nodeId, Long userId, String status);

    /** FR-M6-10 补救优先级提示列表（TopN 已逾期未完成节点，§6.5）。 */
    List<RemediationHintDTO> getRemediationHints(Long userId);

    /** FR-M6-11 整体完成度统计（按 stage 分组）。 */
    ProgressSummaryDTO getProgressSummary(Long userId);
}
