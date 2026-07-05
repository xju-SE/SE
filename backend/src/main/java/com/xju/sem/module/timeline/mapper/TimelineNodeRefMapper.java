package com.xju.sem.module.timeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.timeline.entity.TimelineNodeRef;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * timeline_node_ref 的 MyBatis-Plus Mapper。该表无 deleted 列，引用维护走"物理删旧插新"的
 * 覆盖式重建；{@link #countByRef} 是供 M2/M3/M5 详情页展示"被 N 个成长时间线节点引用"的对外契约
 * 底层实现（§6.7）。
 */
@Mapper
public interface TimelineNodeRefMapper extends BaseMapper<TimelineNodeRef> {

    /** 取某节点下全部引用（按 id 升序=插入顺序=展示顺序）。 */
    @Select("SELECT * FROM timeline_node_ref WHERE node_id = #{nodeId} ORDER BY id ASC")
    List<TimelineNodeRef> listByNode(@Param("nodeId") Long nodeId);

    /** 覆盖式重建前清空某节点全部引用（物理删除）。 */
    @Delete("DELETE FROM timeline_node_ref WHERE node_id = #{nodeId}")
    int deleteByNode(@Param("nodeId") Long nodeId);

    /** §6.7 对外契约底层：统计引用某对象的节点引用条数（供 M3 P09 等）。 */
    @Select("SELECT COUNT(*) FROM timeline_node_ref WHERE ref_type = #{refType} AND ref_id = #{refId}")
    long countByRef(@Param("refType") String refType, @Param("refId") Long refId);
}
