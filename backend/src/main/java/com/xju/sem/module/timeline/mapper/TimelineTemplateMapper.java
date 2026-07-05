package com.xju.sem.module.timeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.timeline.entity.TimelineTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * timeline_template 的 MyBatis-Plus Mapper。发布态流转走"带 WHERE status=? 的 CAS UPDATE"
 * （schema 无 version 列），受影响行数为 0 即前置状态不满足/并发冲突。§6.2 解析与 NULL 场景查重
 * 用显式 SQL（DB 侧对 major_tag_id IS NULL 不去重，且 schema 该表无唯一索引，故全部在应用层兜底）。
 */
@Mapper
public interface TimelineTemplateMapper extends BaseMapper<TimelineTemplate> {

    /** §6.2 解析：指定专业 × 路线的已发布模板。 */
    @Select("SELECT * FROM timeline_template WHERE major_tag_id = #{majorTagId} AND route_type = #{routeType} "
            + "AND status = 'PUBLISHED' AND deleted = 0 LIMIT 1")
    TimelineTemplate findPublishedSpecific(@Param("majorTagId") Long majorTagId, @Param("routeType") String routeType);

    /** §6.2 解析兜底：全专业通用（major_tag_id IS NULL）的已发布模板。 */
    @Select("SELECT * FROM timeline_template WHERE major_tag_id IS NULL AND route_type = #{routeType} "
            + "AND status = 'PUBLISHED' AND deleted = 0 LIMIT 1")
    TimelineTemplate findPublishedGeneric(@Param("routeType") String routeType);

    /** FR-M6-01 查重：同 route_type 下是否已存在全专业通用模板（NULL 场景，补 DB 无唯一索引之缺）。 */
    @Select("SELECT COUNT(*) FROM timeline_template WHERE major_tag_id IS NULL AND route_type = #{routeType} "
            + "AND deleted = 0")
    long countGeneric(@Param("routeType") String routeType);

    /** FR-M6-01 查重：同 major × route_type 是否已存在模板（非 NULL 场景，schema 无唯一索引，应用层兜底）。 */
    @Select("SELECT COUNT(*) FROM timeline_template WHERE major_tag_id = #{majorTagId} AND route_type = #{routeType} "
            + "AND deleted = 0")
    long countByMajorRoute(@Param("majorTagId") Long majorTagId, @Param("routeType") String routeType);

    /** 发布态 CAS：仅当当前状态==from 时置为 to。用于 publish（DRAFT/OFFLINE→PUBLISHED）、offline（PUBLISHED→OFFLINE）。 */
    @Update("UPDATE timeline_template SET status = #{to} WHERE id = #{id} AND status = #{from} AND deleted = 0")
    int casStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to);
}
