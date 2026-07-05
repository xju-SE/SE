package com.xju.sem.module.opportunity.service;

import com.xju.sem.module.opportunity.dto.response.TeamMemberDTO;

import java.util.List;

/** 队伍成员 Service 接口：申请加入 / 审批 / 退出 / 移除（FR-M5-13~16）。 */
public interface TeamMemberService {

    /** FR-M5-13 申请加入队伍；REJECTED/LEFT 允许原地重新申请（upsert 回 APPLYING）。 */
    TeamMemberDTO apply(Long teamId, Long userId);

    /** FR-M5-14① 审批通过（队长）；含 current_size CAS 防超员 + 满员自动转 FULL，单事务原子完成。 */
    TeamMemberDTO approve(Long teamId, Long userId, Long operatorId);

    /** FR-M5-14② 审批拒绝（队长）。 */
    TeamMemberDTO reject(Long teamId, Long userId, Long operatorId);

    /** FR-M5-15 成员主动退出（队长不可退出，只能 end 整个队伍）。 */
    void quit(Long teamId, Long userId);

    /** FR-M5-16 队长移除成员。 */
    void remove(Long teamId, Long userId, Long operatorId);

    /** 成员列表：队长可见全部（含 APPLYING 待审批），其余仅见 JOINED + 自己的记录。 */
    List<TeamMemberDTO> listMembers(Long teamId, Long viewerUserId);
}
