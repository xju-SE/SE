package com.xju.sem.module.help.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.help.dto.request.SubmitAnswerRequest;
import com.xju.sem.module.help.dto.response.HelpAnswerDTO;
import com.xju.sem.module.help.service.HelpAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 回答 Controller：提交模板化回答（挂在求助单下）、编辑本人回答、采纳最佳回答。
 * 越权与状态校验在 {@link HelpAnswerService}。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HelpAnswerController {

    private final HelpAnswerService helpAnswerService;

    /** FR-M4-06 提交三段式回答（须已认证）。 */
    @PostMapping("/help-tickets/{ticketId}/answers")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<HelpAnswerDTO> submit(@PathVariable Long ticketId,
                                        @Valid @RequestBody SubmitAnswerRequest request) {
        return Result.ok(helpAnswerService.submitAnswer(ticketId, SecurityUtil.currentUserId(), request));
    }

    /** FR-M4-07 编辑本人回答（未被采纳前）。 */
    @PutMapping("/help-answers/{id}")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<HelpAnswerDTO> edit(@PathVariable Long id,
                                      @Valid @RequestBody SubmitAnswerRequest request) {
        return Result.ok(helpAnswerService.editAnswer(id, SecurityUtil.currentUserId(), request));
    }

    /** FR-M4-10 采纳该回答为最佳回答（求助人本人）。 */
    @PatchMapping("/help-answers/{id}/adopt")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<Map<String, Object>> adopt(@PathVariable Long id,
                                             @RequestParam Long ticketId) {
        helpAnswerService.adopt(ticketId, id, SecurityUtil.currentUserId());
        Map<String, Object> body = new HashMap<>();
        body.put("ticketStatus", "ADOPTED");
        body.put("adoptedAnswerId", id);
        return Result.ok(body);
    }
}
