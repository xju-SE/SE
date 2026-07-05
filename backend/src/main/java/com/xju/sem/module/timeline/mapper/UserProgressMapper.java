package com.xju.sem.module.timeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.timeline.entity.UserProgress;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * user_progress 的 MyBatis-Plus Mapper。该表无 deleted 列，无逻辑删除。核心批量方法
 * {@link #listByUser}（一次性查出本人全部进度供聚合视图批量比对，避免逐节点单查）、
 * {@link #findConfirmedRoute}（§6.3 由进度反查当前生效分化路线）、{@link #upsert}/{@link #insertIgnore}
 * （同一行只本人写，直接 upsert，不用乐观锁）。
 */
@Mapper
public interface UserProgressMapper extends BaseMapper<UserProgress> {

    /** 一次性取本人全部进度（§6.4 聚合视图/§6.5 补救算法批量比对）。 */
    @Select("SELECT * FROM user_progress WHERE user_id = #{userId}")
    List<UserProgress> listByUser(@Param("userId") Long userId);

    /** 取某模板下全部用户的进度（§FR-M6-12 专业级完成度统计）。 */
    @Select("SELECT up.* FROM user_progress up JOIN timeline_node tn ON up.node_id = tn.id "
            + "WHERE tn.template_id = #{templateId} AND tn.deleted = 0")
    List<UserProgress> listByTemplate(@Param("templateId") Long templateId);

    /**
     * §6.3 反查用户当前生效的分化路线：取其名下"非 UNDECIDED 且属已发布模板"的进度中最近一条
     * （按 up.id 降序 = 插入先后；schema 该表无 created_at，用自增主键代序）的 route_type。
     * 返回 null 表示尚未选择分化路线（仍处/停留在 UNDECIDED）。
     */
    @Select("SELECT tt.route_type FROM user_progress up "
            + "JOIN timeline_node tn ON up.node_id = tn.id "
            + "JOIN timeline_template tt ON tn.template_id = tt.id "
            + "WHERE up.user_id = #{userId} AND tt.route_type <> 'UNDECIDED' "
            + "AND tt.status = 'PUBLISHED' AND tt.deleted = 0 AND tn.deleted = 0 "
            + "ORDER BY up.id DESC LIMIT 1")
    String findConfirmedRoute(@Param("userId") Long userId);

    /** upsert 单条进度（标记/切换）：命中 uk_user_node 则更新 status 与 marked_at。 */
    @Insert("INSERT INTO user_progress(user_id, node_id, status, marked_at) "
            + "VALUES(#{userId}, #{nodeId}, #{status}, #{markedAt}) "
            + "ON DUPLICATE KEY UPDATE status = VALUES(status), marked_at = VALUES(marked_at)")
    int upsert(UserProgress progress);

    /** 批量初始化用 INSERT IGNORE：已存在则保留原有进度（§6.3 幂等切换/§6.6 懒初始化）。 */
    @Insert("INSERT IGNORE INTO user_progress(user_id, node_id, status) "
            + "VALUES(#{userId}, #{nodeId}, 'NOT_STARTED')")
    int insertIgnore(@Param("userId") Long userId, @Param("nodeId") Long nodeId);
}
