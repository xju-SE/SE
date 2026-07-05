package com.xju.sem.module.help.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.help.dto.request.SubmitFollowupRequest;
import com.xju.sem.module.help.dto.response.HelpFollowupDTO;
import com.xju.sem.module.help.service.HelpFollowupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 追问 Controller（按求助单组织线程，对应 schema help_followup 无 target_answer_id）：
 * 提交追问/回复（按当前用户身份自动判定），查看某求助单的追问线程。
 */
@RestController
@RequestMapping("/api/v1/help-tickets/{ticketId}/followups")
@RequiredArgsConstructor
public class HelpFollowupController {

    private final HelpFollowupService helpFollowupService;

    /** FR-M4-08 / FR-M4-09 提交追问或回复（须已认证；限次与身份判定在 Service）。 */
    @PostMapping
    @PreAuthorize("@authGuard.isVerified()")
    public Result<HelpFollowupDTO> submit(@PathVariable Long ticketId,
                                          @Valid @RequestBody SubmitFollowupRequest request) {
        return Result.ok(helpFollowupService.submitFollowup(ticketId, SecurityUtil.currentUserId(), request.getContent()));
    }

    /** 查看某求助单的追问线程（时间正序，登录用户）。 */
    @GetMapping
    @PreAuthorize("@authGuard.isLogin()")
    public Result<List<HelpFollowupDTO>> list(@PathVariable Long ticketId) {
        return Result.ok(helpFollowupService.listFollowups(ticketId, SecurityUtil.currentUserId()));
    }
}
