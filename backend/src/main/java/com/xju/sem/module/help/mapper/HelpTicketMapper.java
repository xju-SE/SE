package com.xju.sem.module.help.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.help.entity.HelpTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * help_ticket 的 MyBatis-Plus Mapper。
 * 状态流转一律走"带 WHERE status=? 条件的 CAS UPDATE"（schema 无 version 列），受影响行数为 0
 * 即判定并发冲突/前置状态不满足；统计卡聚合以 SQL 级完成，不做"读出再算"。
 */
@Mapper
public interface HelpTicketMapper extends BaseMapper<HelpTicket> {

    /** 通用状态 CAS：仅当当前状态==from 时置为 to。用于 OPEN→MATCHED、ANSWERED→ADOPTED 等。 */
    @Update("UPDATE help_ticket SET status = #{to} WHERE id = #{id} AND status = #{from} AND deleted = 0")
    int casStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to);

    /** 首条回答到达：OPEN/MATCHED → ANSWERED（幂等，已 ANSWERED 及以后不再改）。 */
    @Update("UPDATE help_ticket SET status = 'ANSWERED' WHERE id = #{id} AND status IN ('OPEN','MATCHED') AND deleted = 0")
    int markAnswered(@Param("id") Long id);

    /** 关闭：任意非 CLOSED 态 → CLOSED（手动关闭 / 定时超时关闭共用）。 */
    @Update("UPDATE help_ticket SET status = 'CLOSED' WHERE id = #{id} AND status <> 'CLOSED' AND deleted = 0")
    int closeTicket(@Param("id") Long id);

    /**
     * 治理端复核恢复（供 {@code HelpTicketService#restoreTicket} 调用）：deleted 1→0。
     * 用注解 SQL 绕开 MyBatis-Plus 对 BaseMapper 方法自动追加的 {@code deleted = 0} 过滤条件，
     * 否则常规 update/selectById 均看不到已被逻辑删除（隐藏）的行。
     */
    @Update("UPDATE help_ticket SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreTicket(@Param("id") Long id);

    /**
     * 追问计数原子自增，且仅当未达上限时才 +1（限次追问的并发安全实现，见 §6.3）。
     * 受影响行数为 0 表示已达上限或单不存在，由 Service 转 LIMIT_EXCEEDED。
     */
    @Update("UPDATE help_ticket SET followup_count = followup_count + 1 " +
            "WHERE id = #{id} AND deleted = 0 AND followup_count < #{limit}")
    int incrementFollowupCountIfBelowLimit(@Param("id") Long id, @Param("limit") int limit);

    /** 统计卡：本专业待解决数（OPEN/MATCHED/ANSWERED）。 */
    @Select("SELECT COUNT(*) FROM help_ticket WHERE deleted = 0 AND major_tag_id = #{majorTagId} " +
            "AND status IN ('OPEN','MATCHED','ANSWERED')")
    long countOpenByMajor(@Param("majorTagId") Long majorTagId);

    /** 统计卡：本专业已解决数（ADOPTED/CLOSED）。 */
    @Select("SELECT COUNT(*) FROM help_ticket WHERE deleted = 0 AND major_tag_id = #{majorTagId} " +
            "AND status IN ('ADOPTED','CLOSED')")
    long countResolvedByMajor(@Param("majorTagId") Long majorTagId);

    /**
     * 统计卡：本专业平均响应时长（小时）——求助创建到首条回答。无回答样本时返回 null。
     * 子查询按 ticket 取最早回答时间，再对差值求平均，全程 SQL 级聚合。
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(HOUR, t.created_at, fa.first_at)) FROM help_ticket t " +
            "JOIN (SELECT ticket_id, MIN(created_at) AS first_at FROM help_answer WHERE deleted = 0 GROUP BY ticket_id) fa " +
            "ON fa.ticket_id = t.id WHERE t.deleted = 0 AND t.major_tag_id = #{majorTagId}")
    Double avgResponseHoursByMajor(@Param("majorTagId") Long majorTagId);

    /** 定时超时关闭扫描（§6.5 任务二）：超 N 天且从未采纳的 OPEN/MATCHED/ANSWERED 单。 */
    @Select("SELECT * FROM help_ticket WHERE deleted = 0 AND status IN ('OPEN','MATCHED','ANSWERED') " +
            "AND created_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    java.util.List<HelpTicket> selectTimeoutClosable(@Param("days") int days);

    /** 定时宽限关闭扫描（§6.5 任务二）：采纳后（以 updated_at 近似采纳时刻）超 N 天的 ADOPTED 单。 */
    @Select("SELECT * FROM help_ticket WHERE deleted = 0 AND status = 'ADOPTED' " +
            "AND updated_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    java.util.List<HelpTicket> selectAdoptedGraceClosable(@Param("days") int days);

    /**
     * 路由重试扫描（§6.5 任务一）：仍 OPEN/MATCHED、零回答、且创建已超 {@code minMinutes} 分钟的单
     * （超时门槛避免与创建时的即时路由争抢同一单）。
     */
    @Select("SELECT * FROM help_ticket t WHERE t.deleted = 0 AND t.status IN ('OPEN','MATCHED') " +
            "AND t.created_at < DATE_SUB(NOW(), INTERVAL #{minMinutes} MINUTE) " +
            "AND NOT EXISTS (SELECT 1 FROM help_answer a WHERE a.ticket_id = t.id AND a.deleted = 0)")
    java.util.List<HelpTicket> selectRetryable(@Param("minMinutes") int minMinutes);
}
