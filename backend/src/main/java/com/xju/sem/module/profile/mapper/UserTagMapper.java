package com.xju.sem.module.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.profile.entity.UserTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * user_tag 的 MyBatis-Plus Mapper。覆盖式更新采用物理删旧插新（表无 deleted 列）。
 * 本表被 M4 求助-校友路由匹配只读引用（M2 对外以 listUserTags 契约暴露，不外借 Mapper）。
 */
@Mapper
public interface UserTagMapper extends BaseMapper<UserTag> {
}
