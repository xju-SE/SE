package com.xju.sem.module.help.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.help.dto.request.CloseTicketRequest;
import com.xju.sem.module.help.dto.request.CreateHelpTicketRequest;
import com.xju.sem.module.help.dto.request.HelpTicketQuery;
import com.xju.sem.module.help.dto.response.HelpRouteDTO;
import com.xju.sem.module.help.dto.response.HelpTicketDTO;
import com.xju.sem.module.help.dto.response.HelpTicketDetailDTO;
import com.xju.sem.module.help.dto.response.HelpTicketListDTO;
import com.xju.sem.module.help.service.HelpRouteService;
import com.xju.sem.module.help.service.HelpTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 求助单 Controller：发布/列表/详情/撤回/关闭/路由记录查看。仅做参数校验、@PreAuthorize 与转发，
 * 业务规则（快照、状态机、越权校验）在 {@link HelpTicketService}/{@link HelpRouteService}。
 */
@RestController
@RequestMapping("/api/v1/help-tickets")
@RequiredArgsConstructor
public class HelpTicketController {

    private final HelpTicketService helpTicketService;
    private final HelpRouteService helpRouteService;

    /** FR-M4-01 发布求助单（须已认证）。 */
    @PostMapping
    @PreAuthorize("@authGuard.isVerified()")
    public Result<HelpTicketDTO> create(@Valid @RequestBody CreateHelpTicketRequest request) {
        return Result.ok(helpTicketService.createTicket(SecurityUtil.currentUserId(), request));
    }

    /** FR-M4-04 浏览求助单列表（本专业高频，登录用户）。 */
    @GetMapping
    @PreAuthorize("@authGuard.isLogin()")
    public Result<HelpTicketListDTO> list(
            @RequestParam(required = false) Long majorTagId,
            @RequestParam(required = false) Long questionTypeTagId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        HelpTicketQuery query = new HelpTicketQuery();
        query.setMajorTagId(majorTagId);
        query.setQuestionTypeTagId(questionTypeTagId);
        query.setStatus(status);
        query.setSortBy(sortBy);
        query.setPage(page);
        query.setSize(size);
        return Result.ok(helpTicketService.listTickets(query, SecurityUtil.currentUserId()));
    }

    /** FR-M4-05 求助单详情（含回答与追问线程，登录用户）。 */
    @GetMapping("/{id}")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<HelpTicketDetailDTO> detail(@PathVariable Long id) {
        return Result.ok(helpTicketService.getDetail(id, SecurityUtil.currentUserId()));
    }

    /** FR-M4-13 撤回求助单（求助人本人，仅未有回答时）。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<Void> withdraw(@PathVariable Long id) {
        helpTicketService.withdraw(id, SecurityUtil.currentUserId());
        return Result.ok();
    }

    /** FR-M4-12 关闭求助单（求助人本人）。 */
    @PatchMapping("/{id}/close")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<Void> close(@PathVariable Long id,
                              @RequestBody(required = false) CloseTicketRequest request) {
        String reason = request == null ? null : request.getCloseReason();
        helpTicketService.close(id, SecurityUtil.currentUserId(), reason);
        return Result.ok();
    }

    /** 查看路由匹配记录（诊断/复盘，求助人本人/ADMIN）。 */
    @GetMapping("/{id}/routes")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<List<HelpRouteDTO>> routes(@PathVariable Long id) {
        return Result.ok(helpRouteService.listRoutes(id, SecurityUtil.currentUserId()));
    }
}
