package com.xju.sem.module.opportunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
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
import com.xju.sem.module.opportunity.service.TeamMemberService;
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
 * 队伍成员 Service 实现：申请加入 / 审批 / 退出 / 移除（§6.5）。
 *
 * <p><b>并发控制</b>：{@link #approve} 在单个 {@code @Transactional} 方法内完成
 * {@code team.current_size} 的 CAS 递增（防超员）+ {@code team_member.join_status} CAS 更新 +
 * 必要时 {@code team.status→FULL} 的连带更新，任一步失败（CAS 返回 0 行）抛异常触发整体回滚，
 * 与 05 详细设计 §9"approve 单事务原子完成"的要求一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamMemberServiceImpl implements TeamMemberService {

    private final TeamMemberMapper teamMemberMapper;
    private final TeamMapper teamMapper;
    private final OpportunityMapper opportunityMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    private static final String REF_TYPE = "TEAM";

    @Override
    @Transactional
    public TeamMemberDTO apply(Long teamId, Long userId) {
        Team team = requireTeam(teamId);
        if (userId.equals(team.getLeaderId())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "队长无需申请加入本队");
        }
        if (!TeamStatus.RECRUITING.name().equals(team.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "队伍当前不支持申请加入（非招募中）");
        }
        if (!userService.isVerified(userId)) {
            throw new BusinessException(ResultCode.NOT_VERIFIED, "身份未认证，无法申请加入队伍");
        }
        if (team.getOpportunityId() != null) {
            Opportunity opp = opportunityMapper.selectById(team.getOpportunityId());
            if (opp == null || (!OpportunityStatus.ONGOING.name().equals(opp.getStatus())
                    && !OpportunityStatus.CLOSING_SOON.name().equals(opp.getStatus()))) {
                throw new BusinessException(ResultCode.STATE_CONFLICT, "所属机会已截止或不存在");
            }
        }
        TeamMember existing = findMember(teamId, userId);
        if (existing == null) {
            existing = new TeamMember();
            existing.setTeamId(teamId);
            existing.setUserId(userId);
            existing.setMemberRole(TeamMemberRole.MEMBER.name());
            existing.setJoinStatus(TeamMemberJoinStatus.APPLYING.name());
            teamMemberMapper.insert(existing);
        } else if (TeamMemberJoinStatus.REJECTED.name().equals(existing.getJoinStatus())
                || TeamMemberJoinStatus.LEFT.name().equals(existing.getJoinStatus())) {
            // 允许重新申请：原地 upsert 回 APPLYING（§6.5），与 knowledge_feedback upsert 语义一致
            existing.setJoinStatus(TeamMemberJoinStatus.APPLYING.name());
            teamMemberMapper.updateById(existing);
        } else {
            throw new BusinessException(ResultCode.DUPLICATE, "已在队伍中或已有待处理申请");
        }
        notifySafe(team.getLeaderId(), "收到新的组队申请",
                "有用户申请加入你的队伍《" + team.getTitle() + "》", teamId);
        return toDTO(existing);
    }

    @Override
    @Transactional
    public TeamMemberDTO approve(Long teamId, Long userId, Long operatorId) {
        Team team = requireTeam(teamId);
        if (!operatorId.equals(team.getLeaderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有队长可审批加入申请");
        }
        if (!TeamStatus.RECRUITING.name().equals(team.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "队伍当前非招募中，无法审批");
        }
        TeamMember member = findMember(teamId, userId);
        if (member == null || !TeamMemberJoinStatus.APPLYING.name().equals(member.getJoinStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该加入申请不存在或已处理");
        }
        int capRows = teamMapper.incrementIfBelowCapacity(teamId);
        if (capRows == 0) {
            throw new BusinessException(ResultCode.LIMIT_EXCEEDED, "队伍已满员");
        }
        int mRows = teamMemberMapper.casJoinStatus(member.getId(),
                TeamMemberJoinStatus.APPLYING.name(), TeamMemberJoinStatus.JOINED.name());
        if (mRows == 0) {
            // 与上面的 current_size 递增同一事务，抛异常触发整体回滚
            throw new BusinessException(ResultCode.STATE_CONFLICT, "申请状态已变化，请刷新后重试");
        }
        Team refreshed = teamMapper.selectById(teamId);
        if (refreshed.getCurrentSize() >= refreshed.getCapacity()) {
            teamMapper.casStatus(teamId, TeamStatus.RECRUITING.name(), TeamStatus.FULL.name());
            log.info("team {} 满员自动转 FULL（full_reason=AUTO，仅日志留痕）", teamId);
        }
        member.setJoinStatus(TeamMemberJoinStatus.JOINED.name());
        notifySafe(userId, "加入申请已通过", "你申请加入的队伍《" + team.getTitle() + "》已通过审批", teamId);
        return toDTO(member);
    }

    @Override
    @Transactional
    public TeamMemberDTO reject(Long teamId, Long userId, Long operatorId) {
        Team team = requireTeam(teamId);
        if (!operatorId.equals(team.getLeaderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有队长可审批加入申请");
        }
        TeamMember member = findMember(teamId, userId);
        if (member == null || !TeamMemberJoinStatus.APPLYING.name().equals(member.getJoinStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该加入申请不存在或已处理");
        }
        int rows = teamMemberMapper.casJoinStatus(member.getId(),
                TeamMemberJoinStatus.APPLYING.name(), TeamMemberJoinStatus.REJECTED.name());
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "申请状态已变化，请刷新后重试");
        }
        member.setJoinStatus(TeamMemberJoinStatus.REJECTED.name());
        notifySafe(userId, "加入申请未通过",
                "你申请加入的队伍《" + team.getTitle() + "》未通过审批，可重新申请", teamId);
        return toDTO(member);
    }

    @Override
    @Transactional
    public void quit(Long teamId, Long userId) {
        Team team = requireTeam(teamId);
        if (userId.equals(team.getLeaderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "队长不可退出队伍，如需终止请使用解散/结束操作");
        }
        TeamMember member = findMember(teamId, userId);
        if (member == null || !TeamMemberJoinStatus.JOINED.name().equals(member.getJoinStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "你不在该队伍中");
        }
        int rows = teamMemberMapper.casJoinStatus(member.getId(),
                TeamMemberJoinStatus.JOINED.name(), TeamMemberJoinStatus.LEFT.name());
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "状态已变化，请刷新后重试");
        }
        teamMapper.decrementIfAboveZero(teamId);
        // 空出名额后尝试回退 FULL→RECRUITING；schema 无 full_reason 列，无法区分自动满员/队长手动锁定，
        // 本期统一回退（相对 05 详细设计 §6.6"仅 AUTO 才回退"的简化，见实现说明）
        teamMapper.casStatus(teamId, TeamStatus.FULL.name(), TeamStatus.RECRUITING.name());
    }

    @Override
    @Transactional
    public void remove(Long teamId, Long userId, Long operatorId) {
        Team team = requireTeam(teamId);
        if (!operatorId.equals(team.getLeaderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有队长可移除成员");
        }
        if (userId.equals(team.getLeaderId())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "队长不可移除自己");
        }
        TeamMember member = findMember(teamId, userId);
        if (member == null || !TeamMemberJoinStatus.JOINED.name().equals(member.getJoinStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该成员不在队伍中");
        }
        int rows = teamMemberMapper.casJoinStatus(member.getId(),
                TeamMemberJoinStatus.JOINED.name(), TeamMemberJoinStatus.LEFT.name());
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "状态已变化，请刷新后重试");
        }
        teamMapper.decrementIfAboveZero(teamId);
        teamMapper.casStatus(teamId, TeamStatus.FULL.name(), TeamStatus.RECRUITING.name());
        notifySafe(userId, "已被移出队伍", "你已被队长移出队伍《" + team.getTitle() + "》", teamId);
    }

    @Override
    public List<TeamMemberDTO> listMembers(Long teamId, Long viewerUserId) {
        Team team = requireTeam(teamId);
        List<TeamMember> all = teamMemberMapper.selectList(new QueryWrapper<TeamMember>()
                .eq("team_id", teamId).orderByAsc("created_at"));
        boolean isLeader = viewerUserId != null && viewerUserId.equals(team.getLeaderId());
        return all.stream()
                .filter(m -> isLeader
                        || TeamMemberJoinStatus.JOINED.name().equals(m.getJoinStatus())
                        || (viewerUserId != null && viewerUserId.equals(m.getUserId())))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Team requireTeam(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "队伍不存在");
        }
        return team;
    }

    private TeamMember findMember(Long teamId, Long userId) {
        return teamMemberMapper.selectOne(new QueryWrapper<TeamMember>()
                .eq("team_id", teamId).eq("user_id", userId));
    }

    private void notifySafe(Long userId, String title, String content, Long teamId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, "SYSTEM", title, content, REF_TYPE, teamId);
        } catch (Exception e) {
            log.warn("队伍{}通知发送失败: {}", teamId, e.getMessage());
        }
    }

    private String userName(Long userId) {
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

    private TeamMemberDTO toDTO(TeamMember m) {
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
}
