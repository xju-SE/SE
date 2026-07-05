package com.xju.sem.module.user.controller;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.common.security.LoginUser;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.user.dto.AuthApplicationDTO;
import com.xju.sem.module.user.dto.ResubmitAuthApplicationRequest;
import com.xju.sem.module.user.dto.SubmitAuthApplicationRequest;
import com.xju.sem.module.user.service.AuthApplicationService;
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
 * 认证申请（申请人视角）：提交 / 我的历史 / 详情 / 撤回 / 重新提交 / 担保确认。
 * 终审 approve/reject/return 由 M7 治理端 Controller 直接调用本模块 Service，不在此暴露。
 */
@RestController
@RequestMapping("/api/v1/auth-applications")
@RequiredArgsConstructor
public class AuthApplicationController {

    private final AuthApplicationService authApplicationService;

    /** 提交认证申请（登录即可，无需已认证——正是为获取认证）。 */
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','ALUMNI')")
    public Result<AuthApplicationDTO> submit(@Valid @RequestBody SubmitAuthApplicationRequest request) {
        return Result.ok(authApplicationService.submit(SecurityUtil.currentUserId(), request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','ALUMNI')")
    public Result<PageResult<AuthApplicationDTO>> myApplications(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(authApplicationService.pageMine(SecurityUtil.currentUserId(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<AuthApplicationDTO> detail(@PathVariable Long id) {
        AuthApplicationDTO dto = authApplicationService.getById(id);
        LoginUser lu = SecurityUtil.current();
        if (!lu.isAdmin() && !lu.getUserId().equals(dto.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该认证申请");
        }
        return Result.ok(dto);
    }

    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('STUDENT','ALUMNI')")
    public Result<AuthApplicationDTO> withdraw(@PathVariable Long id) {
        return Result.ok(authApplicationService.withdraw(id, SecurityUtil.currentUserId()));
    }

    @PatchMapping("/{id}/resubmit")
    @PreAuthorize("hasAnyRole('STUDENT','ALUMNI')")
    public Result<AuthApplicationDTO> resubmit(@PathVariable Long id,
                                               @Valid @RequestBody ResubmitAuthApplicationRequest request) {
        return Result.ok(authApplicationService.resubmit(id, SecurityUtil.currentUserId(), request));
    }

    /** 担保人确认/拒绝：需已认证用户。 */
    @PatchMapping("/{id}/guarantee")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<AuthApplicationDTO> guarantee(@PathVariable Long id,
                                                @RequestParam boolean approve) {
        return Result.ok(authApplicationService.confirmGuarantee(id, SecurityUtil.currentUserId(), approve));
    }
}
