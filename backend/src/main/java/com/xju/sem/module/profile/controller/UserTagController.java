package com.xju.sem.module.profile.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.profile.dto.request.UpdateUserTagsRequest;
import com.xju.sem.module.profile.dto.response.TagDTO;
import com.xju.sem.module.profile.service.UserTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成长标签 Controller（FR-M2-02，STUDENT/ALUMNI 通用）。覆盖式更新，数量上限与类型校验在 Service。
 */
@RestController
@RequestMapping("/api/v1/user-tags")
@RequiredArgsConstructor
public class UserTagController {

    private final UserTagService userTagService;

    /** 获取本人成长标签。 */
    @GetMapping("/me")
    @PreAuthorize("@authGuard.isVerified() and hasAnyRole('STUDENT','ALUMNI')")
    public Result<List<TagDTO>> getMine() {
        return Result.ok(userTagService.listUserTags(SecurityUtil.currentUserId()));
    }

    /** 覆盖式更新本人成长标签（去重、≤10、类型须 INTEREST/GROWTH）。 */
    @PutMapping("/me")
    @PreAuthorize("@authGuard.isVerified() and hasAnyRole('STUDENT','ALUMNI')")
    public Result<List<TagDTO>> updateMine(@Valid @RequestBody UpdateUserTagsRequest request) {
        return Result.ok(userTagService.updateMyTags(SecurityUtil.currentUserId(), request.getTagIds()));
    }
}
