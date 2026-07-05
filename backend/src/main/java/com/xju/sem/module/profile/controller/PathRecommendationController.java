package com.xju.sem.module.profile.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.profile.dto.request.PathRecommendRequest;
import com.xju.sem.module.profile.dto.response.PathRecommendationDTO;
import com.xju.sem.module.profile.service.PathRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 路径推荐 Controller（FR-M2-09，P07，STUDENT）。纯读计算，不落库；专业必填、其余条件留空自动放宽。
 */
@RestController
@RequestMapping("/api/v1/path-recommendations")
@RequiredArgsConstructor
public class PathRecommendationController {

    private final PathRecommendationService recommendationService;

    @PostMapping
    @PreAuthorize("@authGuard.isVerified() and hasRole('STUDENT')")
    public Result<PathRecommendationDTO> recommend(@Valid @RequestBody PathRecommendRequest request) {
        return Result.ok(recommendationService.recommend(SecurityUtil.currentUserId(), request));
    }
}
