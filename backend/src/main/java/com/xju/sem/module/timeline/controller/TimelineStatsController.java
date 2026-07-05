package com.xju.sem.module.timeline.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.module.timeline.dto.response.MajorTimelineStatsDTO;
import com.xju.sem.module.timeline.service.TimelineTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 专业级时间线完成度统计 Controller（FR-M6-12，供 M7 运营/首页仪表盘，ADMIN）。
 */
@RestController
@RequestMapping("/api/v1/timeline-stats")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TimelineStatsController {

    private final TimelineTemplateService templateService;

    /** 某 major × route 的完成率分布。 */
    @GetMapping("/by-major")
    public Result<MajorTimelineStatsDTO> byMajor(@RequestParam(required = false) Long majorTagId,
                                                 @RequestParam String routeType) {
        return Result.ok(templateService.getMajorTimelineStats(majorTagId, routeType));
    }
}
