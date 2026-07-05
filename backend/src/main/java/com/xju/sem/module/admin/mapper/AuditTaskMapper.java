package com.xju.sem.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.admin.entity.AuditTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * audit_task 的 MyBatis-Plus Mapper。状态流转统一走 SQL 级 CAS（不做"读出再写回"），
 * 与 M3/M5 同一并发控制分工。
 */
public interface AuditTaskMapper extends BaseMapper<AuditTask> {

    /**
     * 终审状态 CAS：仅当仍为 fromStatus（约定为 PENDING）时才生效，返回影响行数——
     * 0 行代表已被他人处理或状态已变化，由调用方转 STATE_CONFLICT。
     */
    @Update("UPDATE audit_task SET status = #{toStatus}, reviewer_id = #{reviewerId}, "
            + "decision_note = #{decisionNote}, decided_at = NOW() "
            + "WHERE id = #{id} AND status = #{fromStatus} AND deleted = 0")
    int casDecide(@Param("id") Long id,
                  @Param("fromStatus") String fromStatus,
                  @Param("toStatus") String toStatus,
                  @Param("reviewerId") Long reviewerId,
                  @Param("decisionNote") String decisionNote);

    /** 按 target_type 分组统计当前 PENDING 数，供队列顶部徽标（FR-M7-01）。 */
    @Select("SELECT target_type AS targetType, COUNT(*) AS cnt FROM audit_task "
            + "WHERE status = 'PENDING' AND deleted = 0 GROUP BY target_type")
    List<TargetTypeCount> countPendingByType();

    /**
     * M7 剩余部分补充（FR-M7-20 运营数据统计）：某 target_type 按 status 分组计数，用于知识候选
     * 审核流水线分布（PENDING/RETURNED/REJECTED/APPROVED），只读聚合本模块自有表，不越权查他表。
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM audit_task "
            + "WHERE target_type = #{targetType} AND deleted = 0 GROUP BY status")
    List<StatusCount> countByTargetTypeGroupStatus(@Param("targetType") String targetType);

    /**
     * M7 剩余部分补充（FR-M7-20/21 审核吞吐量趋势）：按 decided_at 的自然日分组统计已终审
     * （APPROVED/RETURNED/REJECTED，不含 AUTO_APPROVED）数量，供 §6.5 PDAT 峰值对照图。
     */
    @Select("SELECT DATE(decided_at) AS day, COUNT(*) AS cnt FROM audit_task "
            + "WHERE status IN ('APPROVED','RETURNED','REJECTED') AND deleted = 0 "
            + "AND decided_at IS NOT NULL AND DATE(decided_at) BETWEEN #{from} AND #{to} "
            + "GROUP BY DATE(decided_at) ORDER BY day")
    List<DailyCount> dailyDecidedCounts(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
