package com.xju.sem.module.profile.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.profile.dto.request.UpdateStudentProfileRequest;
import com.xju.sem.module.profile.dto.response.StudentProfileDTO;
import com.xju.sem.module.profile.service.StudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 在校生画像 Controller（P04）。仅入参校验转发，业务在 {@link StudentProfileService}。
 * 专业/年级/学号为认证结果只读，本 Controller 不提供其修改入口。
 */
@RestController
@RequestMapping("/api/v1/student-profiles")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    /** 获取本人在校生画像（含成长标签）。 */
    @GetMapping("/me")
    @PreAuthorize("@authGuard.isVerified() and hasRole('STUDENT')")
    public Result<StudentProfileDTO> getMine() {
        return Result.ok(studentProfileService.getProfile(SecurityUtil.currentUserId()));
    }

    /** FR-M2-01 编辑本人画像（GPA/目标城市行业/简介/头像）。 */
    @PutMapping("/me")
    @PreAuthorize("@authGuard.isVerified() and hasRole('STUDENT')")
    public Result<StudentProfileDTO> updateMine(@Valid @RequestBody UpdateStudentProfileRequest request) {
        return Result.ok(studentProfileService.updateProfile(SecurityUtil.currentUserId(), request));
    }
}
