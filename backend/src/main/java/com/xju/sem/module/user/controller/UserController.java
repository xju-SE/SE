package com.xju.sem.module.user.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.user.constant.AuthConst;
import com.xju.sem.module.user.dto.PrivacySettingRequest;
import com.xju.sem.module.user.dto.StatusUpdateRequest;
import com.xju.sem.module.user.dto.UpdateProfileRequest;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.dto.UserDTO;
import com.xju.sem.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户账号管理：当前用户信息、基本信息、隐私设置、担保候选、账号启停。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<UserDTO> me() {
        return Result.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<UserDTO> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(SecurityUtil.currentUserId(), request);
        return Result.ok(userService.getCurrentUser());
    }

    @PatchMapping("/me/privacy")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<UserDTO> updatePrivacy(@Valid @RequestBody PrivacySettingRequest request) {
        userService.updatePrivacySetting(SecurityUtil.currentUserId(), request);
        return Result.ok(userService.getCurrentUser());
    }

    /** 查询同专业且已认证的可担保候选人（供毕业生人工+担保认证选择）。 */
    @GetMapping("/guarantor-candidates")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<List<UserBriefDTO>> guarantorCandidates(@RequestParam String major,
                                                          @RequestParam(required = false) String keyword) {
        return Result.ok(userService.searchGuarantorCandidates(major, keyword));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserDTO> updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody StatusUpdateRequest request) {
        if (AuthConst.UserStatus.DISABLED.equals(request.getStatus())) {
            userService.disableUser(id, request.getReason());
        } else {
            userService.enableUser(id);
        }
        return Result.ok(userService.getById(id));
    }
}
