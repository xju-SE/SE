package com.xju.sem.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.social.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * user_follow 的 MyBatis-Plus Mapper。自定义查询均手动带 {@code deleted = 0}
 * （自定义 @Select 不经过 wrapper，不会被全局逻辑删除配置自动改写）。
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /** 是否已关注：followerId 是否关注了 followeeId。 */
    @Select("SELECT COUNT(*) > 0 FROM user_follow WHERE follower_id = #{followerId} AND followee_id = #{followeeId} AND deleted = 0")
    boolean existsFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /** 粉丝数：被多少人关注。 */
    @Select("SELECT COUNT(*) FROM user_follow WHERE followee_id = #{userId} AND deleted = 0")
    long countFollowers(@Param("userId") Long userId);

    /** 关注数：关注了多少人。 */
    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{userId} AND deleted = 0")
    long countFollowing(@Param("userId") Long userId);
}
