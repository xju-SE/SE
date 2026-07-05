package com.xju.sem.module.opportunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.opportunity.dto.request.CreateTeamRequest;
import com.xju.sem.module.opportunity.dto.request.TeamQuery;
import com.xju.sem.module.opportunity.dto.request.UpdateTeamRequest;
import com.xju.sem.module.opportunity.dto.response.TeamBriefDTO;
import com.xju.sem.module.opportunity.dto.response.TeamDTO;
import com.xju.sem.module.opportunity.dto.response.TeamMemberDTO;
import com.xju.sem.module.opportunity.entity.Opportunity;
import com.xju.sem.module.opportunity.entity.Team;
import com.xju.sem.module.opportunity.entity.TeamMember;
import com.xju.sem.module.opportunity.enums.OpportunityStatus;
import com.xju.sem.module.opportunity.enums.TeamMemberJoinStatus;
import com.xju.sem.module.opportunity.enums.TeamMemberRole;
import com.xju.sem.module.opportunity.enums.TeamStatus;
import com.xju.sem.module.opportunity.mapper.OpportunityMapper;
import com.xju.sem.module.opportunity.mapper.TeamMapper;
import com.xju.sem.module.opportunity.mapper.TeamMemberMapper;
import com.xju.sem.module.opportunity.service.TeamService;
import com.xju.sem.module.notification.service.NotificationService;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 队伍 Service 实现：发起队伍（§6.4）、状态流转（§6.6）、机会 ENDED 级联结束（§6.7）。
 *
 * <p>Team/Opportunity/TeamMember 同属本模块（opportunity 包），彼此以 Mapper 直连属模块内耦合，
 * 不受"跨模块只调 Service 接口"约束；跨模块（{@link UserService}/{@link NotificationService}）
 * 一律走接口。current_size 的并发名额竞争 CAS 见 {@link TeamMemberServiceImpl#approve}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final OpportunityMapper opportunityMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    private static final String REF_TYPE = "TEAM";

    @Override
    @Transactional
    public TeamDTO createTeam(Long opportunityId, Long leaderId, CreateTeamRequest request) {
        if (!userService.isVerified(leaderId)) {
            throw new BusinessException(ResultCode.NOT_VERIFIED, "身份未认证，无法发起队伍");
        }
        Opportunity opp = null;
        if (opportunityId != null) {
            opp = opportunityMapper.selectById(opportunityId);
            if (opp == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "机会不存在");
            }
            if (!OpportunityStatus.ONGOING.name().equals(opp.getStatus())
                    && !OpportunityStatus.CLOSING_SOON.name().equals(opp.getStatus())) {
                throw new BusinessException(ResultCode.STATE_CONFLICT, "该机会当前不支持发起队伍（未开放或已截止）");
            }
            // S19：仅 team_required=1 的机会允许围绕其发起队伍；无 opportunityId 的自由组队不受此限。
            if (opp.getTeamRequired() == null || opp.getTeamRequired() != 1) {
                throw new BusinessException(30022, "该机会不支持组队");
            }
        }
        Team team = new Team();
        team.setOpportunityId(opportunityId);
        team.setLeaderId(leaderId);
        team.setTitle(request.getTitle());
        team.setDescription(request.getDescription());
        team.setNeedDesc(request.getNeedDesc());
        team.setCapacity(request.getCapacity());
        team.setCurrentSize(1);
        team.setStatus(TeamStatus.RECRUITING.name());
        teamMapper.insert(team);

        TeamMember leader = new TeamMember();
        leader.setTeamId(team.getId());
        leader.setUserId(leaderId);
        leader.setMemberRole(TeamMemberRole.LEADER.name());
        leader.setJoinStatus(TeamMemberJoinStatus.JOINED.name());
        teamMemberMapper.insert(leader);

        return toDTO(team, opp, leaderId, List.of(leader));
    }

    @Override
    @Transactional
    public TeamDTO updateTeam(Long id, Long operatorId, UpdateTeamRequest request) {
        Team team = requireExisting(id);
        if (!operatorId.equals(team.getLeaderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有队长可编辑队伍信息");
        }
        if (TeamStatus.ENDED.name().equals(team.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "已结束的队伍不可编辑");
        }
        if (request.getCapacity() < team.getCurrentSize()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "人数上限不能小于当前已批准人数");
        }
        team.setTitle(request.getTitle());
        team.setDescription(request.getDescription());
        team.setNeedDesc(request.getNeedDesc());
        team.setCapacity(request.getCapacity());
        teamMapper.updateById(team);
        return toDTO(team, fetchOpportunity(team.getOpportunityId()), operatorId, null);
    }

    @Override
    public TeamDTO getById(Long id, Long viewerUserId) {
        Team team = requireExisting(id);
        List<TeamMember> members = teamMemberMapper.selectList(
                new QueryWrapper<TeamMember>().eq("team_id", id).orderByAsc("created_at"));
        return toDTO(team, fetchOpportunity(team.getOpportunityId()), viewerUserId, filterVisible(team, members, viewerUserId));
    }

    @Override
    public PageResult<TeamBriefDTO> list(TeamQuery query) {
        if (StringUtils.hasText(query.getStatus()) && !TeamStatus.isValid(query.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "队伍状态取值不合法");
        }
        QueryWrapper<Team> qw = new QueryWrapper<>();
        if (query.getOpportunityId() != null) {
            qw.eq("opportunity_id", query.getOpportunityId());
        }
        qw.eq("status", StringUtils.hasText(query.getStatus()) ? query.getStatus() : TeamStatus.RECRUITING.name());
        if (StringUtils.hasText(query.getKeyword())) {
            qw.like("title", query.getKeyword().trim());
        }
        qw.orderByDesc("created_at");
        IPage<Team> result = teamMapper.selectPage(pageOf(query.getPage(), query.getSize()), qw);
        return toBriefPage(result);
    }

    @Override
    public PageResult<TeamBriefDTO> pageMine(Long userId, String status, int page, int size) {
        List<Long> memberTeamIds = teamMemberMapper.selectList(
                        new QueryWrapper<TeamMember>().eq("user_id", userId))
                .stream().map(TeamMember::getTeamId).distinct().collect(Collectors.toList());
        QueryWrapper<Team> qw = new QueryWrapper<>();
        qw.and(w -> {
            w.eq("leader_id", userId);
            if (!memberTeamIds.isEmpty()) {
                w.or(w2 -> w2.in("id", memberTeamIds));
            }
        });
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        qw.orderByDesc("updated_at");
        IPage<Team> result = teamMapper.selectPage(pageOf(page, size), qw);
        return toBriefPage(result);
    }

    @Override
    @Transactional
    public TeamDTO lock(Long id, Long operatorId) {
        Team team = requireExisting(id);
        requireLeader(team, operatorId);
        int rows = teamMapper.casStatus(id, TeamStatus.RECRUITING.name(), TeamStatus.FULL.name());
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅招募中的队伍可停止招募");
        }
        team.setStatus(TeamStatus.FULL.name());
        log.info("team {} locked (manual, full_reason=MANUAL_LOCK 仅日志留痕) by leader {}", id, operatorId);
        return toDTO(team, fetchOpportunity(team.getOpportunityId()), operatorId, null);
    }

    @Override
    @Transactional
    public TeamDTO start(Long id, Long operatorId) {
        Team team = requireExisting(id);
        requireLeader(team, operatorId);
        int rows = teamMapper.casStatus(id, TeamStatus.FULL.name(), TeamStatus.ONGOING.name());
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅已满员的队伍可开始协作");
        }
        team.setStatus(TeamStatus.ONGOING.name());
        return toDTO(team, fetchOpportunity(team.getOpportunityId()), operatorId, null);
    }

    @Override
    @Transactional
    public TeamDTO end(Long id, Long operatorId, String reason) {
        Team team = requireExisting(id);
        requireLeader(team, operatorId);
        if (TeamStatus.ENDED.name().equals(team.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "队伍已结束");
        }
        int rows = teamMapper.casStatus(id, team.getStatus(), TeamStatus.ENDED.name());
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "队伍状态已变更，请刷新后重试");
        }
        team.setStatus(TeamStatus.ENDED.name());
        log.info("team {} ended by leader {}, reason={}", id, operatorId, reason);
        notifyJoinedMembers(id, "队伍已结束", "你所在的队伍《" + team.getTitle() + "》已标记结束");
        return toDTO(team, fetchOpportunity(team.getOpportunityId()), operatorId, null);
    }

    @Override
    public void endAllByOpportunity(Long opportunityId, String reason) {
        if (opportunityId == null) {
            return;
        }
        List<Team> teams = teamMapper.selectList(new QueryWrapper<Team>()
                .eq("opportunity_id", opportunityId)
                .in("status", List.of(TeamStatus.RECRUITING.name(), TeamStatus.FULL.name(), TeamStatus.ONGOING.name())));
        for (Team t : teams) {
            int rows = teamMapper.casStatus(t.getId(), t.getStatus(), TeamStatus.ENDED.name());
            if (rows == 0) {
                continue;
            }
            log.info("team {} ended by opportunity {} archive (reason={})", t.getId(), opportunityId, reason);
            notifyJoinedMembers(t.getId(), "所属机会已结束",
                    "你所在的队伍《" + t.getTitle() + "》所属机会已结束，队伍自动归档");
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void requireLeader(Team team, Long operatorId) {
        if (!operatorId.equals(team.getLeaderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有队长可执行该操作");
        }
    }

    private Team requireExisting(Long id) {
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "队伍不存在");
        }
        return team;
    }

    private Opportunity fetchOpportunity(Long opportunityId) {
        return opportunityId == null ? null : opportunityMapper.selectById(opportunityId);
    }

    private List<TeamMember> filterVisible(Team team, List<TeamMember> members, Long viewerUserId) {
        boolean isLeader = viewerUserId != null && viewerUserId.equals(team.getLeaderId());
        if (isLeader) {
            return members;
        }
        return members.stream()
                .filter(m -> TeamMemberJoinStatus.JOINED.name().equals(m.getJoinStatus())
                        || (viewerUserId != null && viewerUserId.equals(m.getUserId())))
                .collect(Collectors.toList());
    }

    private void notifyJoinedMembers(Long teamId, String title, String content) {
        List<TeamMember> joined = teamMemberMapper.selectList(new QueryWrapper<TeamMember>()
                .eq("team_id", teamId).eq("join_status", TeamMemberJoinStatus.JOINED.name()));
        for (TeamMember m : joined) {
            try {
                notificationService.send(m.getUserId(), "SYSTEM", title, content, REF_TYPE, teamId);
            } catch (Exception e) {
                log.warn("队伍{}成员{}通知发送失败: {}", teamId, m.getUserId(), e.getMessage());
            }
        }
    }

    private Page<Team> pageOf(Integer page, Integer size) {
        int p = (page == null || page <= 0) ? 1 : page;
        int s = (size == null || size <= 0) ? 10 : Math.min(size, 50);
        return new Page<>(p, s);
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            UserBriefDTO brief = userService.getBrief(userId);
            if (brief == null) {
                return null;
            }
            return StringUtils.hasText(brief.getRealName()) ? brief.getRealName() : brief.getUsername();
        } catch (Exception e) {
            log.debug("获取用户摘要失败 userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    private TeamDTO toDTO(Team t, Opportunity opp, Long viewerUserId, List<TeamMember> members) {
        boolean isLeader = viewerUserId != null && viewerUserId.equals(t.getLeaderId());
        boolean hasActiveMembership = false;
        if (members != null) {
            hasActiveMembership = members.stream().anyMatch(m -> viewerUserId != null && viewerUserId.equals(m.getUserId())
                    && (TeamMemberJoinStatus.APPLYING.name().equals(m.getJoinStatus())
                        || TeamMemberJoinStatus.JOINED.name().equals(m.getJoinStatus())));
        }
        boolean joinable = viewerUserId != null && !isLeader && !hasActiveMembership
                && TeamStatus.RECRUITING.name().equals(t.getStatus());
        return TeamDTO.builder()
                .id(t.getId())
                .opportunityId(t.getOpportunityId())
                .opportunityTitle(opp == null ? null : opp.getTitle())
                .leaderId(t.getLeaderId())
                .leaderName(userName(t.getLeaderId()))
                .title(t.getTitle())
                .description(t.getDescription())
                .needDesc(t.getNeedDesc())
                .capacity(t.getCapacity())
                .currentSize(t.getCurrentSize())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .members(members == null ? null : members.stream().map(this::toMemberDTO).collect(Collectors.toList()))
                .isLeader(isLeader)
                .joinable(joinable)
                .build();
    }

    private TeamMemberDTO toMemberDTO(TeamMember m) {
        return TeamMemberDTO.builder()
                .id(m.getId())
                .teamId(m.getTeamId())
                .userId(m.getUserId())
                .userName(userName(m.getUserId()))
                .memberRole(m.getMemberRole())
                .joinStatus(m.getJoinStatus())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private TeamBriefDTO toBrief(Team t) {
        Opportunity opp = fetchOpportunity(t.getOpportunityId());
        return TeamBriefDTO.builder()
                .id(t.getId())
                .opportunityId(t.getOpportunityId())
                .opportunityTitle(opp == null ? null : opp.getTitle())
                .leaderId(t.getLeaderId())
                .leaderName(userName(t.getLeaderId()))
                .title(t.getTitle())
                .needDesc(t.getNeedDesc())
                .capacity(t.getCapacity())
                .currentSize(t.getCurrentSize())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private PageResult<TeamBriefDTO> toBriefPage(IPage<Team> page) {
        List<TeamBriefDTO> records = page.getRecords().stream().map(this::toBrief).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
