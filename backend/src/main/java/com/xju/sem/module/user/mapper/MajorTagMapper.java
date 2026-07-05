package com.xju.sem.module.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 只读查询全局 tag 表，把认证申请里的“专业文本”解析成 major_tag_id。
 * <p>说明：tag 表归属 M2，本模块仅做只读名称匹配（无 TagService 契约时的兜底），
 * 不写入、不持有 Tag 实体，属受控的跨模块只读例外，详见实现说明 §4。
 */
@Mapper
public interface MajorTagMapper {

    /** 按 MAJOR 类型的标签名精确匹配，返回标签 id；无匹配返回 null。 */
    @Select("SELECT id FROM tag WHERE tag_type = 'MAJOR' AND tag_name = #{name} AND deleted = 0 LIMIT 1")
    Long findMajorIdByName(@Param("name") String name);
}
