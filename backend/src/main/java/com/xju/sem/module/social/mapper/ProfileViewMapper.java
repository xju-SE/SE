package com.xju.sem.module.social.mapper;

import com.xju.sem.module.social.dto.PublicUserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 他人主页公开资料只读投影：直连 user / student_profile / alumni_profile / tag / user_tag /
 * user_follow / user_badge / knowledge_entry / help_ticket 多表 JOIN 组装，不新建表。
 *
 * <p>不 extends BaseMapper（本 Mapper 无对应单一实体）。所有自定义 @Select 均手动带
 * {@code deleted = 0}（自定义 SQL 不经过 wrapper，不会被全局逻辑删除配置自动改写）；
 * {@code user_tag} 表本身无 deleted 列，故只对 JOIN 到的 tag 表过滤 deleted。
 */
@Mapper
public interface ProfileViewMapper {

    /**
     * 基础信息：username/role/authStatus/bio/major/grade/avatarUrl。
     * bio/avatarUrl 取 student_profile 与 alumni_profile 中存在的一份（COALESCE）；
     * major 由 major_tag_id JOIN tag 取 tag_name；grade 仅 STUDENT 有值。
     * 目标用户不存在或已删除时返回 null。
     */
    @Select("SELECT u.id AS user_id, u.username AS username, u.role AS role, u.auth_status AS auth_status, "
            + "COALESCE(sp.bio, ap.bio) AS bio, "
            + "t.tag_name AS major, "
            + "sp.grade_level AS grade, "
            + "COALESCE(sp.avatar_url, ap.avatar_url) AS avatar_url "
            + "FROM user u "
            + "LEFT JOIN student_profile sp ON sp.user_id = u.id AND sp.deleted = 0 "
            + "LEFT JOIN alumni_profile ap ON ap.user_id = u.id AND ap.deleted = 0 "
            + "LEFT JOIN tag t ON t.id = COALESCE(sp.major_tag_id, ap.major_tag_id) AND t.deleted = 0 "
            + "WHERE u.id = #{userId} AND u.deleted = 0")
    PublicUserDTO baseInfo(@Param("userId") Long userId);

    /** 用户标签名称列表。 */
    @Select("SELECT t.tag_name FROM user_tag ut JOIN tag t ON t.id = ut.tag_id AND t.deleted = 0 "
            + "WHERE ut.user_id = #{userId}")
    List<String> tags(@Param("userId") Long userId);

    /** 公开徽章名称列表（hidden=0），置顶优先。 */
    @Select("SELECT badge_name FROM user_badge WHERE user_id = #{userId} AND hidden = 0 AND deleted = 0 "
            + "ORDER BY pinned DESC, awarded_at DESC")
    List<String> badges(@Param("userId") Long userId);

    /** 粉丝数：被多少人关注。 */
    @Select("SELECT COUNT(*) FROM user_follow WHERE followee_id = #{userId} AND deleted = 0")
    long countFollowers(@Param("userId") Long userId);

    /** 关注数：关注了多少人。 */
    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{userId} AND deleted = 0")
    long countFollowing(@Param("userId") Long userId);

    /** 查看者 viewerId 是否已关注 userId。 */
    @Select("SELECT COUNT(*) > 0 FROM user_follow WHERE follower_id = #{viewerId} AND followee_id = #{userId} "
            + "AND deleted = 0")
    boolean isFollowing(@Param("viewerId") Long viewerId, @Param("userId") Long userId);

    /** 发布数：该用户发布的知识条目 + 求助单总数。 */
    @Select("SELECT (SELECT COUNT(*) FROM knowledge_entry WHERE author_id = #{userId} AND deleted = 0) "
            + "+ (SELECT COUNT(*) FROM help_ticket WHERE asker_id = #{userId} AND deleted = 0)")
    long postCount(@Param("userId") Long userId);
}
