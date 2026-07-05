package com.xju.sem.module.opportunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.opportunity.entity.Opportunity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * opportunity 的 MyBatis-Plus Mapper。状态流转一律走"带 WHERE status=? 条件的 CAS UPDATE"
 * （schema 无 version 列），受影响行数为 0 即判定并发冲突/前置状态不满足，与
 * {@code HelpTicketMapper}/{@code KnowledgeEntryMapper} 同一风格。
 */
@Mapper
public interface OpportunityMapper extends BaseMapper<Opportunity> {

    /** 通用状态 CAS：仅当当前状态==from 时置为 to。用于终审通过/拒绝、手动结束、强制下线。 */
    @Update("UPDATE opportunity SET status = #{to} WHERE id = #{id} AND status = #{from} AND deleted = 0")
    int casStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to);

    /** 定时任务①：ONGOING 且距 deadline &lt;= 阈值小时 → CLOSING_SOON（批量，§6.2）。 */
    @Update("UPDATE opportunity SET status = 'CLOSING_SOON' WHERE deleted = 0 AND status = 'ONGOING' " +
            "AND deadline <= DATE_ADD(NOW(), INTERVAL #{hours} HOUR)")
    int advanceOngoingToClosingSoon(@Param("hours") int hours);

    /** 定时任务①兜底：ONGOING/CLOSING_SOON 且 deadline 已过 → CLOSED（批量）。 */
    @Update("UPDATE opportunity SET status = 'CLOSED' WHERE deleted = 0 AND status IN ('ONGOING','CLOSING_SOON') " +
            "AND deadline <= NOW()")
    int advanceToClosed();

    /** 定时任务②扫描候选集：CLOSED 且已超归档窗口（deadline+archiveDays）的机会。 */
    @Select("SELECT * FROM opportunity WHERE deleted = 0 AND status = 'CLOSED' " +
            "AND deadline <= DATE_SUB(NOW(), INTERVAL #{archiveDays} DAY)")
    List<Opportunity> selectArchivable(@Param("archiveDays") int archiveDays);

    /** 单条归档 CAS：仅当仍为 CLOSED 时才转 ENDED，防止与并发的手动结束重复处理。 */
    @Update("UPDATE opportunity SET status = 'ENDED' WHERE id = #{id} AND status = 'CLOSED'")
    int archiveIfClosed(@Param("id") Long id);
}
