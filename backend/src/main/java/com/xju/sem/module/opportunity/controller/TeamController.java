package com.xju.sem.module.opportunity.controller;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.opportunity.dto.request.CreateTeamRequest;
import com.xju.sem.module.opportunity.dto.request.EndTeamRequest;
import com.xju.sem.module.opportunity.dto.request.TeamQuery;
import com.xju.sem.module.opportunity.dto.request.UpdateTeamRequest;
import com.xju.sem.module.opportunity.dto.response.TeamBriefDTO;
import com.xju.sem.module.opportunity.dto.response.TeamDTO;
import com.xju.sem.module.opportunity.dto.response.TeamMemberDTO;
import com.xju.sem.module.opportunity.service.TeamMemberService;
import com.xju.sem.module.opportunity.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 队伍 Controller（P15 组队广场 + 队伍详情），含成员申请/审批子路径（{@code TeamMemberController}
 * 未独立建类，作为本类子路径方法实现，05 详细设计 §9 允许的两种组织方式之一）。
 * 仅做入参校验转发，业务逻辑在 {@link TeamService}/{@link TeamMemberService}。
 */
@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamMemberService teamMemberService;

    /** FR-M5-18 组队广场列表（默认仅 RECRUITING）。 */
    @GetMapping("/api/v1/teams")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<PageResult<TeamBriefDTO>> list(
            @RequestParam(required = false) Long opportunityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        TeamQuery query = new TeamQuery();
        query.setOpportunityId(opportunityId);
        query.setStatus(status);
        query.setKeyword(keyword);
        query.setPage(page);
        query.setSize(size);
        return Result.ok(teamService.list(query));
    }

    /** FR-M5-20 我发起/加入的队伍列表。放在 /{id} 之前声明，避免与路径变量匹配冲突。 */
    @GetMapping("/api/v1/teams/mine")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<PageResult<TeamBriefDTO>> mine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(teamService.pageMine(SecurityUtil.currentUserId(), status, page, size));
    }

    /** FR-M5-19 队伍详情（含成员列表，队长额外可见 APPLYING 待审批）。 */
    @GetMapping("/api/v1/teams/{id}")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<TeamDTO> getById(@PathVariable Long id) {
        return Result.ok(teamService.getById(id, SecurityUtil.currentUserId()));
    }

    /** FR-M5-11 发起队伍（挂靠具体机会）。 */
    @PostMapping("/api/v1/opportunities/{opportunityId}/teams")
    @PreAuthorize("@authGuard.isVerified() and hasAnyRole('STUDENT','ALUMNI')")
    public Result<TeamDTO> createUnderOpportunity(@PathVariable Long opportunityId,
                                                   @Valid @RequestBody CreateTeamRequest request) {
        return Result.ok(teamService.createTeam(opportunityId, SecurityUtil.currentUserId(), request));
    }

    /** 发起自由组队（不关联任何机会，schema team.opportunity_id 注释"可空,自由组队"）。 */
    @PostMapping("/api/v1/teams")
    @PreAuthorize("@authGuard.isVerified() and hasAnyRole('STUDENT','ALUMNI')")
    public Result<TeamDTO> createFree(@Valid @RequestBody CreateTeamRequest request) {
        return Result.ok(teamService.createTeam(null, SecurityUtil.currentUserId(), request));
    }

    /** FR-M5-12 编辑队伍信息（队长）。 */
    @PutMapping("/api/v1/teams/{id}")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<TeamDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateTeamRequest request) {
        return Result.ok(teamService.updateTeam(id, SecurityUtil.currentUserId(), request));
    }

    /** FR-M5-17① 停止招募：RECRUITING → FULL。 */
    @PatchMapping("/api/v1/teams/{id}/lock")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<TeamDTO> lock(@PathVariable Long id) {
        return Result.ok(teamService.lock(id, SecurityUtil.currentUserId()));
    }

    /** FR-M5-17② 开始协作：FULL → ONGOING。 */
    @PatchMapping("/api/v1/teams/{id}/start")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<TeamDTO> start(@PathVariable Long id) {
        return Result.ok(teamService.start(id, SecurityUtil.currentUserId()));
    }

    /** FR-M5-17③ 标记结束/解散。 */
    @PatchMapping("/api/v1/teams/{id}/end")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<TeamDTO> end(@PathVariable Long id, @RequestBody(required = false) EndTeamRequest request) {
        String reason = request == null ? null : request.getReason();
        return Result.ok(teamService.end(id, SecurityUtil.currentUserId(), reason));
    }

    /** FR-M5-13 申请加入队伍。 */
    @PostMapping("/api/v1/teams/{id}/members")
    @PreAuthorize("@authGuard.isVerified() and hasAnyRole('STUDENT','ALUMNI')")
    public Result<TeamMemberDTO> apply(@PathVariable Long id) {
        return Result.ok(teamMemberService.apply(id, SecurityUtil.currentUserId()));
    }

    /** 成员列表（队长额外见 APPLYING 待审批申请）。 */
    @GetMapping("/api/v1/teams/{id}/members")
    @PreAuthorize("@authGuard.isLogin()")
    public Result<List<TeamMemberDTO>> listMembers(@PathVariable Long id) {
        return Result.ok(teamMemberService.listMembers(id, SecurityUtil.currentUserId()));
    }

    /** FR-M5-14① 审批通过（队长）。 */
    @PatchMapping("/api/v1/teams/{id}/members/{userId}/approve")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<TeamMemberDTO> approveMember(@PathVariable Long id, @PathVariable Long userId) {
        return Result.ok(teamMemberService.approve(id, userId, SecurityUtil.currentUserId()));
    }

    /** FR-M5-14② 审批拒绝（队长）。 */
    @PatchMapping("/api/v1/teams/{id}/members/{userId}/reject")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<TeamMemberDTO> rejectMember(@PathVariable Long id, @PathVariable Long userId) {
        return Result.ok(teamMemberService.reject(id, userId, SecurityUtil.currentUserId()));
    }

    /** FR-M5-15/16 退出（本人）/移除成员（队长），由路径 userId 是否等于当前登录人区分。 */
    @DeleteMapping("/api/v1/teams/{id}/members/{userId}")
    @PreAuthorize("@authGuard.isVerified()")
    public Result<Void> leaveOrRemove(@PathVariable Long id, @PathVariable Long userId) {
        Long me = SecurityUtil.currentUserId();
        if (me.equals(userId)) {
            teamMemberService.quit(id, me);
        } else {
            teamMemberService.remove(id, userId, me);
        }
        return Result.ok();
    }
}
