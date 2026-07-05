package com.xju.sem.module.admin.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.module.admin.dto.AuditThroughputStatsDTO;
import com.xju.sem.module.admin.dto.OperationOverviewDTO;
import com.xju.sem.module.admin.service.OperationStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 运营数据统计看板（P18 Tab⑤，FR-M7-20/21）。全部接口仅 ADMIN 可用。 */
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatsController {

    private final OperationStatsService operationStatsService;

    /** FR-M7-20 运营数据总览，dateFrom/dateTo 缺省默认近 7 日。 */
    @GetMapping("/overview")
    public Result<OperationOverviewDTO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return Result.ok(operationStatsService.getOverview(dateFrom, dateTo));
    }

    /** FR-M7-20/21 审核吞吐量趋势（含 §6.5 PDAT 峰值估算对照）。 */
    @GetMapping("/audit-throughput")
    public Result<AuditThroughputStatsDTO> auditThroughput(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return Result.ok(operationStatsService.getAuditThroughput(dateFrom, dateTo));
    }
}
