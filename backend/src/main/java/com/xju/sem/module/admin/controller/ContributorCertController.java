package com.xju.sem.module.admin.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.admin.dto.ApplyContributorCertRequest;
import com.xju.sem.module.admin.dto.AuditTaskDTO;
import com.xju.sem.module.admin.service.ContributorCertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 贡献者认证申请入口（FR-M7-18，入口标注"预计耗时：约2分钟"由前端文案负责）。 */
@RestController
@RequestMapping("/api/v1/contributor-cert-applications")
@RequiredArgsConstructor
public class ContributorCertController {

    private final ContributorCertService contributorCertService;

    @PostMapping
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<AuditTaskDTO> apply(@Valid @RequestBody ApplyContributorCertRequest request) {
        return Result.ok(contributorCertService.apply(SecurityUtil.currentUserId(), request));
    }
}
