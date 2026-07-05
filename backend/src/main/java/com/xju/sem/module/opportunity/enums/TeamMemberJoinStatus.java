package com.xju.sem.module.opportunity.enums;

/**
 * 队伍成员加入状态（team_member.join_status，schema 注释：APPLYING/JOINED/REJECTED/LEFT）。
 * 与 05 详细设计 §3.3 的 PENDING/APPROVED/REJECTED/QUIT/REMOVED 五态相比，本 schema 将"主动退出"
 * 与"被队长移除"合并为同一个 LEFT 终态（不持久化 reviewed_by/reviewed_at，无法从数据上区分二者），
 * 是相对 05 设计的裁剪（详见实现说明"假设与简化"）；队长记录创建即为 JOINED。
 */
public enum TeamMemberJoinStatus {
    APPLYING,
    JOINED,
    REJECTED,
    LEFT;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (TeamMemberJoinStatus s : values()) {
            if (s.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
