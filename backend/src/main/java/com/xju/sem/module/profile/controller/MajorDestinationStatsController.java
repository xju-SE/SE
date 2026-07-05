package com.xju.sem.module.profile.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.module.profile.dto.response.MajorDestinationStatsDTO;
import com.xju.sem.module.profile.service.MajorDestinationStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 按专业去向统计 Controller（FR-M2-08，P06 仪表盘）。须已认证访问；样本充足性由后端
 * sampleSufficient 标志驱动，前端不做本地估算。可选二级维度下钻（行业/院校）。
 */
@RestController
@RequestMapping("/api/v1/major-destination-stats")
@RequiredArgsConstructor
public class MajorDestinationStatsController {

    private final MajorDestinationStatsService statsService;

    /**
     * 按专业聚合去向统计。传入 destinationType + dimension 时附带二级维度下钻：
     * dimension=INDUSTRY（EMPLOY 行业分布）/ SCHOOL（POSTGRAD 院校分布），小桶合并 OTHER。
     */
    @GetMapping
    @PreAuthorize("@authGuard.isVerified()")
    public Result<MajorDestinationStatsDTO> stats(
            @RequestParam Long majorTagId,
            @RequestParam(required = false) String destinationType,
            @RequestParam(required = false) String dimension) {
        if (destinationType != null && dimension != null) {
            return Result.ok(statsService.getStats(majorTagId, destinationType, dimension));
        }
        return Result.ok(statsService.getStats(majorTagId));
    }
}
