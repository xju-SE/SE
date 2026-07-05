package com.xju.sem.module.opportunity.enums;

/**
 * 队伍状态机（team.status，schema 注释：RECRUITING/FULL/ONGOING/ENDED，四态，与 05 详细设计 §4.2 一致）。
 *
 * <pre>
 *   [*] ──发起组队──▶ RECRUITING
 *   RECRUITING ──满员(自动) 或 队长手动停止招募──▶ FULL
 *   FULL ──队长确认开始协作──▶ ONGOING
 *   RECRUITING/FULL ──队长解散──▶ ENDED
 *   ONGOING ──队长标记完成 或 所属机会ENDED级联──▶ ENDED
 *   ENDED ──▶ 终态
 * </pre>
 * FULL→RECRUITING 的隐式回退（成员退出后空出名额）见 Service 层说明，不在本枚举画为正式跃迁。
 */
public enum TeamStatus {
    RECRUITING,
    FULL,
    ONGOING,
    ENDED;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (TeamStatus s : values()) {
            if (s.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
