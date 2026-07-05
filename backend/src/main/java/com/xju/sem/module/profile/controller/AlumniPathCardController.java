package com.xju.sem.module.profile.controller;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.LoginUser;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.profile.dto.request.HidePathCardRequest;
import com.xju.sem.module.profile.dto.request.PathCardRequest;
import com.xju.sem.module.profile.dto.request.UpdateVisibilityRequest;
import com.xju.sem.module.profile.dto.response.AlumniPathCardDTO;
import com.xju.sem.module.profile.dto.response.PathVisibilityDTO;
import com.xju.sem.module.profile.dto.response.VisiblePathCardDTO;
import com.xju.sem.module.profile.service.AlumniPathCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 校友路径卡 Controller（P05/P06）。仅入参校验转发，状态机/分支校验/脱敏在
 * {@link AlumniPathCardService}。发布/撤回为本人 PATCH；下架/复核为 ADMIN PATCH（供 M7 治理）。
 * 浏览/详情须已认证（GUEST 不可访问，对齐"看校友隐私字段需认证"权限矩阵）。
 */
@RestController
@RequestMapping("/api/v1/alumni-path-cards")
@RequiredArgsConstructor
public class AlumniPathCardController {

    private final AlumniPathCardService pathCardService;

    // ---------------- 本人 CRUD ----------------

    @PostMapping
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<AlumniPathCardDTO> create(@Valid @RequestBody PathCardRequest request) {
        return Result.ok(pathCardService.create(SecurityUtil.currentUserId(), request));
    }

    /** 本人路径卡列表（P05）。声明在 /{id} 之前，避免路径变量匹配冲突。 */
    @GetMapping("/mine")
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<List<AlumniPathCardDTO>> mine() {
        return Result.ok(pathCardService.listMine(SecurityUtil.currentUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<AlumniPathCardDTO> update(@PathVariable Long id,
                                            @Valid @RequestBody PathCardRequest request) {
        LoginUser lu = SecurityUtil.current();
        return Result.ok(pathCardService.update(id, lu.getUserId(), lu.isAdmin(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authGuard.isVerified() and hasAnyRole('ALUMNI','ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        LoginUser lu = SecurityUtil.current();
        pathCardService.delete(id, lu.getUserId(), lu.isAdmin());
        return Result.ok();
    }

    // ---------------- 状态机 ----------------

    @PatchMapping("/{id}/publish")
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<Map<String, String>> publish(@PathVariable Long id) {
        return Result.ok(status(pathCardService.publish(id, SecurityUtil.currentUserId())));
    }

    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<Map<String, String>> withdraw(@PathVariable Long id) {
        return Result.ok(status(pathCardService.withdraw(id, SecurityUtil.currentUserId())));
    }

    /** 举报下架（供 M7 调用），ADMIN 权限。 */
    @PatchMapping("/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> hide(@PathVariable Long id,
                                            @RequestBody(required = false) HidePathCardRequest request) {
        String reason = request == null ? null : request.getReason();
        return Result.ok(status(pathCardService.hidePathCardByReport(id, SecurityUtil.currentUserId(), reason)));
    }

    /** 复核恢复，ADMIN 权限。 */
    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> restore(@PathVariable Long id) {
        return Result.ok(status(pathCardService.restorePathCard(id, SecurityUtil.currentUserId())));
    }

    // ---------------- 可见性配置 ----------------

    @GetMapping("/{id}/visibility")
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<List<PathVisibilityDTO>> getVisibility(@PathVariable Long id) {
        return Result.ok(pathCardService.getVisibility(id, SecurityUtil.currentUserId()));
    }

    @PutMapping("/{id}/visibility")
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<List<PathVisibilityDTO>> updateVisibility(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateVisibilityRequest request) {
        return Result.ok(pathCardService.updateVisibility(id, SecurityUtil.currentUserId(), request));
    }

    // ---------------- 浏览（脱敏）----------------

    @GetMapping
    @PreAuthorize("@authGuard.isVerified()")
    public Result<PageResult<VisiblePathCardDTO>> list(
            @RequestParam(required = false) Long majorTagId,
            @RequestParam(required = false) String destinationType,
            @RequestParam(required = false) Integer gradYearFrom,
            @RequestParam(required = false) Integer gradYearTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        LoginUser lu = SecurityUtil.current();
        return Result.ok(pathCardService.pageList(majorTagId, destinationType, gradYearFrom, gradYearTo,
                lu.getUserId(), lu.isAdmin(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<VisiblePathCardDTO> detail(@PathVariable Long id) {
        LoginUser lu = SecurityUtil.current();
        return Result.ok(pathCardService.getDetail(id, lu.getUserId(), lu.isAdmin()));
    }

    private Map<String, String> status(String status) {
        return Collections.singletonMap("status", status);
    }
}
