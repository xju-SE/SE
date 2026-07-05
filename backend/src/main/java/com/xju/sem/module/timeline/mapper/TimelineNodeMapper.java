package com.xju.sem.module.timeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.timeline.entity.TimelineNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * timeline_node 的 MyBatis-Plus Mapper。按模板取节点后在 Service 层以 {@code Stage.order + order_no}
 * 排序（stage 为字符串枚举，非字典序，故不在 SQL 直接 ORDER BY stage）。
 */
@Mapper
public interface TimelineNodeMapper extends BaseMapper<TimelineNode> {

    /** 取某模板下全部未删除节点（排序在 Service 层按学期自然序 + order_no 完成）。 */
    @Select("SELECT * FROM timeline_node WHERE template_id = #{templateId} AND deleted = 0")
    List<TimelineNode> listByTemplate(@Param("templateId") Long templateId);
}
