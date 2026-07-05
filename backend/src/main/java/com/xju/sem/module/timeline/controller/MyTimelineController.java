package com.xju.sem.module.timeline.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.timeline.dto.request.MarkProgressRequest;
import com.xju.sem.module.timeline.dto.response.MyTimelineDTO;
import com.xju.sem.module.timeline.dto.response.ProgressSummaryDTO;
import com.xju.sem.module.timeline.dto.response.RemediationHintDTO;
import com.xju.sem.module.timeline.dto.response.RouteConfirmResultDTO;
import com.xju.sem.module.timeline.dto.response.TimelineNodeDTO;
import com.xju.sem.module.timeline.dto.response.TimelineSummaryCardDTO;
import com.xju.sem.module.timeline.dto.response.UserProgressDTO;
import com.xju.sem.module.timeline.service.UserProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生端成长时间线 Controller（P16，STUDENT）：动态导航聚合视图、首页摘要卡、路线预览/切换、
 * 进度标记、补救优先级、完成度统计。仅做入参校验转发，业务规则在 {@link UserProgressService}。
 * 进度标记端点路径 {@code /api/v1/timeline-nodes/{id}/progress} 与 ADMIN 的节点维护同前缀不同动作，
 * 语义上属"我的进度"，故归本 Controller（STUDENT 门控），与 ADMIN 的 {@code TimelineNodeController} 隔离。
 */
@RestController
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class MyTimelineController {

    private final UserProgressService userProgressService;

    /** FR-M6-05 查看我的成长时间线（动态导航聚合视图）。 */
    @GetMapping("/api/v1/timeline/me")
    public Result<MyTimelineDTO> myTimeline() {
        return Result.ok(userProgressService.getMyTimeline(SecurityUtil.currentUserId()));
    }

    /** 首页仪表盘摘要卡。 */
    @GetMapping("/api/v1/timeline/me/summary-card")
    public Result<TimelineSummaryCardDTO> summaryCard() {
        return Result.ok(userProgressService.getMySummaryCard(SecurityUtil.currentUserId()));
    }

    /** FR-M6-06 预览某路线内容（决策前对比），不写入个人进度。 */
    @GetMapping("/api/v1/timeline/route-preview")
    public Result<List<TimelineNodeDTO>> routePreview(@RequestParam String routeType) {
        return Result.ok(userProgressService.previewRoute(SecurityUtil.currentUserId(), routeType));
    }

    /** FR-M6-07 选择/切换发展路线（UNDECIDED 不可提交）。 */
    @PatchMapping("/api/v1/timeline/me/route")
    public Result<RouteConfirmResultDTO> confirmRoute(@RequestParam String routeType) {
        return Result.ok(userProgressService.confirmRoute(SecurityUtil.currentUserId(), routeType));
    }

    /** FR-M6-08 标记/切换节点个人进度。 */
    @PatchMapping("/api/v1/timeline-nodes/{id}/progress")
    public Result<UserProgressDTO> markProgress(@PathVariable Long id,
                                                @Valid @RequestBody MarkProgressRequest request) {
        return Result.ok(userProgressService.markProgress(id, SecurityUtil.currentUserId(), request.getStatus()));
    }

    /** FR-M6-10 补救优先级提示列表。 */
    @GetMapping("/api/v1/timeline/me/remediation")
    public Result<List<RemediationHintDTO>> remediation() {
        return Result.ok(userProgressService.getRemediationHints(SecurityUtil.currentUserId()));
    }

    /** FR-M6-11 整体完成度统计（按 stage 分组）。 */
    @GetMapping("/api/v1/timeline/me/progress-summary")
    public Result<ProgressSummaryDTO> progressSummary() {
        return Result.ok(userProgressService.getProgressSummary(SecurityUtil.currentUserId()));
    }
}
