package com.xju.sem.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.admin.entity.Report;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * report 的 MyBatis-Plus Mapper。状态流转统一走 SQL 级 CAS，与 audit_task 同一并发控制分工
 * （07 详细设计 §4.2/§9）。
 */
public interface ReportMapper extends BaseMapper<Report> {

    /**
     * 处理状态 CAS：仅当仍为 PENDING 时才生效，返回影响行数——0 行代表已被他人处理，
     * 由调用方转 {@code AdminErrorCode.REPORT_STATE_CONFLICT}。
     */
    @Update("UPDATE report SET status = #{toStatus}, handler_id = #{handlerId}, handle_note = #{handleNote} "
            + "WHERE id = #{id} AND status = 'PENDING' AND deleted = 0")
    int casHandle(@Param("id") Long id, @Param("toStatus") String toStatus,
                  @Param("handlerId") Long handlerId, @Param("handleNote") String handleNote);
}
