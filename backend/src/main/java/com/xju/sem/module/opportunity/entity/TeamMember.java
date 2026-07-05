package com.xju.sem.module.opportunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 队伍成员（表 team_member）。唯一约束 uk_team_user(team_id,user_id)：同一用户对同一队伍仅保留
 * 一条当前关系记录，REJECTED/LEFT 后允许原地 upsert 回 APPLYING 重新申请（见 Service 层 §6.5 逻辑）。
 * BaseEntity 的 createdAt/updatedAt 兼任"申请时间/最近一次审批时间"语义，schema 未开独立的
 * apply_message/reviewed_at/reviewed_by 列（相对 05 设计 §3.3 的裁剪，见实现说明）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("team_member")
public class TeamMember extends BaseEntity {

    private Long teamId;

    private Long userId;

    /** LEADER/MEMBER，见 {@code TeamMemberRole}。 */
    private String memberRole;

    /** APPLYING/JOINED/REJECTED/LEFT，见 {@code TeamMemberJoinStatus}。 */
    private String joinStatus;
}
