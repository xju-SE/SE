package com.xju.sem.module.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.profile.entity.PathVisibility;
import org.apache.ibatis.annotations.Mapper;

/**
 * path_visibility 的 MyBatis-Plus Mapper。读写均走 BaseMapper + LambdaQueryWrapper
 * （按 path_card_id 查/删、按 UK 重建分组），无逻辑删除列，删除为物理删除。
 */
@Mapper
public interface PathVisibilityMapper extends BaseMapper<PathVisibility> {
}
