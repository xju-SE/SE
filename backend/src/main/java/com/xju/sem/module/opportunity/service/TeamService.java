package com.xju.sem.module.opportunity.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.opportunity.dto.request.CreateTeamRequest;
import com.xju.sem.module.opportunity.dto.request.TeamQuery;
import com.xju.sem.module.opportunity.dto.request.UpdateTeamRequest;
import com.xju.sem.module.opportunity.dto.response.TeamBriefDTO;
import com.xju.sem.module.opportunity.dto.response.TeamDTO;

/** 队伍 Service 接口。{@link #endAllByOpportunity} 供 {@code OpportunityStatusScheduler}/{@code OpportunityServiceImpl.end} 级联调用（FR-M5-10）。 */
public interface TeamService {

    /**
     * FR-M5-11 发起队伍；opportunityId 可空（自由组队，不受限），非空时校验其处于
     * ONGOING/CLOSING_SOON 且 team_required=1（S19，否则抛 30022），无 opportunityId 的自由组队仍允许。
     */
    TeamDTO createTeam(Long opportunityId, Long leaderId, CreateTeamRequest request);

    /** FR-M5-12 编辑队伍信息（队长）；capacity 不可小于当前已批准人数。 */
    TeamDTO updateTeam(Long id, Long operatorId, UpdateTeamRequest request);

    /** FR-M5-19 队伍详情（含成员列表，队长额外可见 APPLYING 待审批）。 */
    TeamDTO getById(Long id, Long viewerUserId);

    /** FR-M5-18 组队广场列表，status 为空时默认仅 RECRUITING。 */
    PageResult<TeamBriefDTO> list(TeamQuery query);

    /** FR-M5-20 我发起/加入的队伍列表：leader_id=me OR EXISTS team_member(user_id=me)。 */
    PageResult<TeamBriefDTO> pageMine(Long userId, String status, int page, int size);

    /** FR-M5-17① 停止招募：RECRUITING → FULL（人未满，队长提前锁定）。 */
    TeamDTO lock(Long id, Long operatorId);

    /** FR-M5-17② 开始协作：FULL → ONGOING。 */
    TeamDTO start(Long id, Long operatorId);

    /** FR-M5-17③ 标记结束/解散：任意非终态 → ENDED，通知全体 JOINED 成员。 */
    TeamDTO end(Long id, Long operatorId, String reason);

    /** FR-M5-10 机会 ENDED 级联结束关联队伍（供定时任务/{@code OpportunityService.end} 调用，非用户直接触发）。 */
    void endAllByOpportunity(Long opportunityId, String reason);
}
