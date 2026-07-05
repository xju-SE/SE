package com.xju.sem.module.knowledge.controller;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.LoginUser;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.knowledge.dto.request.CreateKnowledgeEntryRequest;
import com.xju.sem.module.knowledge.dto.request.OfflineRequest;
import com.xju.sem.module.knowledge.dto.request.UpdateKnowledgeEntryRequest;
import com.xju.sem.module.knowledge.dto.response.KnowledgeBriefDTO;
import com.xju.sem.module.knowledge.dto.response.KnowledgeEntryDTO;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 知识条目 Controller：仅做入参校验转发，业务逻辑在 {@link KnowledgeEntryService}。
 * 终审接口（approve/return）不在本 Controller——按 09 设计修订说明，其路由挂载于 M7
 * AdminKnowledgeEntryController，复用本模块 KnowledgeEntryService Bean，不在此重复暴露端点。
 */
@RestController
@RequestMapping("/api/v1/knowledge-entries")
@RequiredArgsConstructor
public class KnowledgeEntryController {

    private final KnowledgeEntryService knowledgeEntryService;

    /** FR-M3-09 列表 + 分类筛选（全角色，含 GUEST，仅见 PUBLISHED）。 */
    @GetMapping
    public Result<PageResult<KnowledgeBriefDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(knowledgeEntryService.list(category, viewerIdOrNull(), page, size));
    }

    /** FR-M3-10 全文搜索（全角色，含 GUEST）。 */
    @GetMapping("/search")
    public Result<PageResult<KnowledgeBriefDTO>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(knowledgeEntryService.search(keyword, category, page, size));
    }

    /** FR-M3-14 我的知识贡献列表。放在 /{id} 之前声明，避免与路径变量匹配冲突。 */
    @GetMapping("/mine")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<PageResult<KnowledgeBriefDTO>> mine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(knowledgeEntryService.pageMine(SecurityUtil.currentUserId(), status, page, size));
    }

    /** FR-M3-11 详情（全角色；非 PUBLISHED 限作者/认领人/ADMIN 可见）。 */
    @GetMapping("/{id}")
    public Result<KnowledgeEntryDTO> getById(@PathVariable Long id) {
        LoginUser lu = SecurityUtil.currentOrNull();
        Long viewerId = lu == null ? null : lu.getUserId();
        boolean isAdmin = lu != null && lu.isAdmin();
        return Result.ok(knowledgeEntryService.getById(id, viewerId, isAdmin));
    }

    /** FR-M3-01 创建原创知识条目。 */
    @PostMapping
    @PreAuthorize("@authGuard.isVerified()")
    public Result<KnowledgeEntryDTO> create(@Valid @RequestBody CreateKnowledgeEntryRequest request) {
        return Result.ok(knowledgeEntryService.create(SecurityUtil.currentUserId(), request));
    }

    /** FR-M3-03 编辑（含对已发布内容发起修订）。 */
    @PutMapping("/{id}")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<KnowledgeEntryDTO> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateKnowledgeEntryRequest request) {
        LoginUser lu = SecurityUtil.current();
        return Result.ok(knowledgeEntryService.update(id, lu.getUserId(), lu.isAdmin(), request));
    }

    /** FR-M3-15 软删除。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<Void> delete(@PathVariable Long id) {
        LoginUser lu = SecurityUtil.current();
        knowledgeEntryService.delete(id, lu.getUserId(), lu.isAdmin());
        return Result.ok();
    }

    /** FR-M3-04 提交审核。 */
    @PatchMapping("/{id}/submit")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<KnowledgeEntryDTO> submit(@PathVariable Long id) {
        LoginUser lu = SecurityUtil.current();
        return Result.ok(knowledgeEntryService.submitForReview(id, lu.getUserId(), lu.isAdmin()));
    }

    /** FR-M3-08 认领内容更新（STUDENT/ALUMNI，已认证）。 */
    @PatchMapping("/{id}/claim")
    @PreAuthorize("@authGuard.isVerified() and hasAnyRole('STUDENT','ALUMNI')")
    public Result<KnowledgeEntryDTO> claim(@PathVariable Long id) {
        return Result.ok(knowledgeEntryService.claim(id, SecurityUtil.currentUserId()));
    }

    /** FR-M3-07 手动下线。 */
    @PatchMapping("/{id}/offline")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<KnowledgeEntryDTO> offline(@PathVariable Long id, @RequestBody(required = false) OfflineRequest request) {
        LoginUser lu = SecurityUtil.current();
        OfflineRequest req = request == null ? new OfflineRequest() : request;
        return Result.ok(knowledgeEntryService.offline(id, lu.getUserId(), lu.isAdmin(), req));
    }

    private Long viewerIdOrNull() {
        LoginUser lu = SecurityUtil.currentOrNull();
        return lu == null ? null : lu.getUserId();
    }
}
