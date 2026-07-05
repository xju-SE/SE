package com.xju.sem.module.opportunity.controller;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.LoginUser;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.opportunity.dto.request.CreateOpportunityRequest;
import com.xju.sem.module.opportunity.dto.request.EndOpportunityRequest;
import com.xju.sem.module.opportunity.dto.request.OpportunityQuery;
import com.xju.sem.module.opportunity.dto.request.UpdateOpportunityRequest;
import com.xju.sem.module.opportunity.dto.response.OpportunityBriefDTO;
import com.xju.sem.module.opportunity.dto.response.OpportunityDTO;
import com.xju.sem.module.opportunity.service.OpportunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 机会 Controller（P13/P14）：仅做入参校验转发，业务逻辑在 {@link OpportunityService}。
 * 终审接口（approve/reject）不在本 Controller——由 M7 治理端统一审核队列
 * （{@code /api/v1/audit-tasks/{id}/decide}）调用本模块 Service，不在此重复暴露端点，
 * 与 {@code KnowledgeEntryController}/{@code AuthApplicationController} 同一模式。
 */
@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    /** FR-M5-06 列表（类型筛选/即将截止/关键字），全角色（含 GUEST，仅见 APPROVED 等价态）。 */
    @GetMapping
    public Result<PageResult<OpportunityBriefDTO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean closingSoon,
            @RequestParam(defaultValue = "false") boolean includeEnded,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        OpportunityQuery query = new OpportunityQuery();
        query.setType(type);
        query.setClosingSoon(closingSoon);
        query.setIncludeEnded(includeEnded);
        query.setKeyword(keyword);
        query.setPage(page);
        query.setSize(size);
        return Result.ok(opportunityService.list(query, viewerIdOrNull()));
    }

    /** P03 首页仪表盘"即将截止机会"卡片只读聚合展示。 */
    @GetMapping("/closing-soon")
    public Result<PageResult<OpportunityBriefDTO>> closingSoon(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(opportunityService.listClosingSoon(Math.min(limit, 20)));
    }

    /** FR-M5-07 详情（全角色；PENDING_REVIEW 限发布人/ADMIN 可见）。 */
    @GetMapping("/{id}")
    public Result<OpportunityDTO> getById(@PathVariable Long id) {
        LoginUser lu = SecurityUtil.currentOrNull();
        Long viewerId = lu == null ? null : lu.getUserId();
        boolean isAdmin = lu != null && lu.isAdmin();
        return Result.ok(opportunityService.getById(id, viewerId, isAdmin));
    }

    /** FR-M5-01 发布机会（ALUMNI/ADMIN，已认证）。 */
    @PostMapping
    @PreAuthorize("@authGuard.isVerified() and (hasRole('ALUMNI') or hasRole('ADMIN'))")
    public Result<OpportunityDTO> create(@Valid @RequestBody CreateOpportunityRequest request) {
        return Result.ok(opportunityService.create(SecurityUtil.currentUserId(), request));
    }

    /** FR-M5-02 编辑机会（发布人/ADMIN）。 */
    @PutMapping("/{id}")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<OpportunityDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateOpportunityRequest request) {
        LoginUser lu = SecurityUtil.current();
        return Result.ok(opportunityService.update(id, lu.getUserId(), lu.isAdmin(), request));
    }

    /** 软删除（发布人/ADMIN）。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<Void> delete(@PathVariable Long id) {
        LoginUser lu = SecurityUtil.current();
        opportunityService.delete(id, lu.getUserId(), lu.isAdmin());
        return Result.ok();
    }

    /** FR-M5-04/05 手动结束/强制下线（发布人本人或 ADMIN，ADMIN 不受当前状态限制）。 */
    @PatchMapping("/{id}/end")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<OpportunityDTO> end(@PathVariable Long id, @RequestBody(required = false) EndOpportunityRequest request) {
        LoginUser lu = SecurityUtil.current();
        String reason = request == null ? null : request.getReason();
        return Result.ok(opportunityService.end(id, lu.getUserId(), lu.isAdmin(), reason));
    }

    /** FR-M5-08 简单报名信令（STUDENT/ALUMNI，已认证）。 */
    @PatchMapping("/{id}/apply")
    @PreAuthorize("@authGuard.isVerified() and hasAnyRole('STUDENT','ALUMNI')")
    public Result<Void> apply(@PathVariable Long id) {
        opportunityService.applySignal(id, SecurityUtil.currentUserId());
        return Result.ok();
    }

    private Long viewerIdOrNull() {
        LoginUser lu = SecurityUtil.currentOrNull();
        return lu == null ? null : lu.getUserId();
    }
}
