package com.xju.sem.module.profile.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.profile.dto.request.UpdateAlumniProfileRequest;
import com.xju.sem.module.profile.dto.response.AlumniProfileDTO;
import com.xju.sem.module.profile.service.AlumniProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 毕业生档案 Controller。徽章授予/计数累加走跨模块 Service 契约（M7/M4 调用），不在本 Controller 暴露端点。
 */
@RestController
@RequestMapping("/api/v1/alumni-profiles")
@RequiredArgsConstructor
public class AlumniProfileController {

    private final AlumniProfileService alumniProfileService;

    /** 获取本人毕业生档案（含成长标签、徽章计数）。 */
    @GetMapping("/me")
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<AlumniProfileDTO> getMine() {
        return Result.ok(alumniProfileService.getMyProfile(SecurityUtil.currentUserId()));
    }

    /** 编辑本人毕业生档案展示字段。 */
    @PutMapping("/me")
    @PreAuthorize("@authGuard.isVerified() and hasRole('ALUMNI')")
    public Result<AlumniProfileDTO> updateMine(@Valid @RequestBody UpdateAlumniProfileRequest request) {
        return Result.ok(alumniProfileService.updateMyProfile(SecurityUtil.currentUserId(), request));
    }
}
