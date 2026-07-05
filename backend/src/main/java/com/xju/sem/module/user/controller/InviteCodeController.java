package com.xju.sem.module.user.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.module.user.dto.BatchInviteCodeRequest;
import com.xju.sem.module.user.dto.InviteCodeCheckDTO;
import com.xju.sem.module.user.service.AuthApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 毕业生邀请码：认领前预检有效性、ADMIN 批量生成。
 */
@RestController
@RequestMapping("/api/v1/invite-codes")
@RequiredArgsConstructor
public class InviteCodeController {

    private final AuthApplicationService authApplicationService;

    @GetMapping("/{code}/check")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<InviteCodeCheckDTO> check(@PathVariable String code) {
        return Result.ok(authApplicationService.checkInviteCode(code));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<String>> batch(@Valid @RequestBody BatchInviteCodeRequest request) {
        return Result.ok(authApplicationService.batchCreateInviteCodes(request));
    }
}
