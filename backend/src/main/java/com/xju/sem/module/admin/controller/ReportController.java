package com.xju.sem.module.admin.controller;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.admin.dto.HandleReportRequest;
import com.xju.sem.module.admin.dto.ReportDTO;
import com.xju.sem.module.admin.dto.ReportQuery;
import com.xju.sem.module.admin.dto.SubmitReportRequest;
import com.xju.sem.module.admin.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报受理（07 详细设计 §5(b)/§9："ReportController：submit（普通用户）/mine（普通用户）/
 * queue/{id}/{id}/handle（ADMIN）"，本次合一实现，方法级区分权限）。
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** FR-M7-09 提交举报：登录用户（含未认证，不含 GUEST）均可提交。 */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<ReportDTO> submit(@Valid @RequestBody SubmitReportRequest request) {
        return Result.ok(reportService.submit(SecurityUtil.currentUserId(), request));
    }

    /** FR-M7-12 我提交的举报记录。 */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ReportDTO>> mine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reportService.pageMine(SecurityUtil.currentUserId(), status, page, size));
    }

    /** FR-M7-10 举报队列（治理端）。 */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<ReportDTO>> queue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        ReportQuery query = new ReportQuery();
        query.setStatus(status);
        query.setTargetType(targetType);
        query.setPage(page);
        query.setSize(size);
        return Result.ok(reportService.pageForAdmin(query));
    }

    /** FR-M7-10 举报详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReportDTO> detail(@PathVariable Long id) {
        return Result.ok(reportService.getById(id));
    }

    /** FR-M7-11 处理举报。 */
    @PatchMapping("/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReportDTO> handle(@PathVariable Long id, @Valid @RequestBody HandleReportRequest request) {
        return Result.ok(reportService.handle(id, SecurityUtil.currentUserId(), request));
    }
}
