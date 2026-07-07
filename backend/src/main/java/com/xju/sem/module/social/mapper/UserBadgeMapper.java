package com.xju.sem.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.social.entity.UserBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * user_badge 的 MyBatis-Plus Mapper。自定义查询/更新均手动带 {@code deleted = 0}
 * （自定义 @Select/@Update 不经过 wrapper，不会被全局逻辑删除配置自动改写）。
 */
@Mapper
public interface UserBadgeMapper extends BaseMapper<UserBadge> {

    /** 公开徽章列表（他人主页可见）：仅 hidden=0，置顶优先、按授予时间倒序。 */
    @Select("SELECT * FROM user_badge WHERE user_id = #{userId} AND deleted = 0 AND hidden = 0 "
            + "ORDER BY pinned DESC, awarded_at DESC")
    List<UserBadge> listPublic(@Param("userId") Long userId);

    /** 本人全部徽章（含隐藏）：置顶优先、按授予时间倒序。 */
    @Select("SELECT * FROM user_badge WHERE user_id = #{userId} AND deleted = 0 "
            + "ORDER BY pinned DESC, awarded_at DESC")
    List<UserBadge> listMine(@Param("userId") Long userId);

    /** 通用列表：includeHidden=false 时仅返回 hidden=0，与 listPublic/listMine 共用同一排序口径。 */
    @Select("SELECT * FROM user_badge WHERE user_id = #{userId} AND deleted = 0 "
            + "AND (#{includeHidden} = true OR hidden = 0) "
            + "ORDER BY pinned DESC, awarded_at DESC")
    List<UserBadge> listByUser(@Param("userId") Long userId, @Param("includeHidden") boolean includeHidden);

    /** 更新置顶/隐藏标记，同时校验归属（WHERE id=? AND user_id=?），防止越权修改他人徽章。 */
    @Update("UPDATE user_badge SET pinned = #{pinned}, hidden = #{hidden} "
            + "WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int updateFlags(@Param("id") Long id, @Param("userId") Long userId,
                     @Param("pinned") Integer pinned, @Param("hidden") Integer hidden);
}
