package com.xju.sem.module.knowledge.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.LoginUser;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.knowledge.dto.request.SubmitFeedbackRequest;
import com.xju.sem.module.knowledge.dto.response.FeedbackSummaryDTO;
import com.xju.sem.module.knowledge.dto.response.KnowledgeFeedbackDTO;
import com.xju.sem.module.knowledge.service.KnowledgeFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 三态评价/纠错 Controller（挂载于知识条目子路径）。 */
@RestController
@RequestMapping("/api/v1/knowledge-entries/{id}/feedbacks")
@RequiredArgsConstructor
public class KnowledgeFeedbackController {

    private final KnowledgeFeedbackService knowledgeFeedbackService;

    /** FR-M3-12 提交/更新三态评价。 */
    @PostMapping
    @PreAuthorize("@authGuard.isVerified()")
    public Result<KnowledgeFeedbackDTO> submit(@PathVariable Long id,
                                               @Valid @RequestBody SubmitFeedbackRequest request) {
        Long userId = SecurityUtil.currentUserId();
        return Result.ok(knowledgeFeedbackService.submitFeedback(id, userId, request.getFeedbackType(), request.getComment()));
    }

    /** FR-M3-13 查看三态评价统计（全角色，含 GUEST）。 */
    @GetMapping("/summary")
    public Result<FeedbackSummaryDTO> summary(@PathVariable Long id) {
        LoginUser lu = SecurityUtil.currentOrNull();
        Long viewerId = lu == null ? null : lu.getUserId();
        return Result.ok(knowledgeFeedbackService.getSummary(id, viewerId));
    }
}
