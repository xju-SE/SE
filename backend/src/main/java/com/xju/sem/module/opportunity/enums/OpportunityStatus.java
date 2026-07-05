package com.xju.sem.module.opportunity.enums;

/**
 * 机会状态机（对齐 schema.sql opportunity.status 注释：
 * PENDING_REVIEW/ONGOING/CLOSING_SOON/CLOSED/ENDED/REJECTED，六态单字段，与 05 详细设计 §3.1/§4.1
 * 的"status(四态) + audit_status(正交三态)"双字段设计不同——本 schema 未开 audit_status 列，
 * 用同一个 status 字段合并表达"审核门"与"对外时间窗口"两层语义，是本实现相对 05 设计的裁剪
 * （详见实现说明"假设与简化"）。
 *
 * <p><b>S18</b>：终审拒绝落到独立的 {@link #REJECTED} 终审态（而非直接归档 ENDED），
 * 发布人可编辑（{@code update()}）后重新提交，回到 PENDING_REVIEW 重新终审，解除
 * "被拒机会不可重提"的限制；REJECTED 与 PENDING_REVIEW 一样仅发布人/ADMIN 可见。
 *
 * <pre>
 *   PENDING_REVIEW ──终审通过──▶ ONGOING/CLOSING_SOON(按deadline即时计算)
 *   PENDING_REVIEW ──终审拒绝──▶ REJECTED
 *   REJECTED ──发布人编辑重新提交(update())──▶ PENDING_REVIEW
 *   ONGOING ──定时任务:临近deadline──▶ CLOSING_SOON ──定时任务:deadline已过──▶ CLOSED
 *   ONGOING/CLOSING_SOON ──定时任务:deadline已过(兜底)──▶ CLOSED
 *   CLOSED ──定时任务:超归档窗口 或 手动结束/强制下线──▶ ENDED
 *   ONGOING/CLOSING_SOON/PENDING_REVIEW/REJECTED ──手动结束/强制下线(ADMIN不受状态限制)──▶ ENDED
 *   ENDED ──▶ 终态，不可再流转
 * </pre>
 */
public enum OpportunityStatus {
    PENDING_REVIEW,
    ONGOING,
    CLOSING_SOON,
    CLOSED,
    ENDED,
    /** S18：终审拒绝态，区别于 ENDED；可编辑重新提交回 PENDING_REVIEW。 */
    REJECTED;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (OpportunityStatus s : values()) {
            if (s.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
