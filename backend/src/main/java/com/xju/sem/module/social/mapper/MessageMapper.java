package com.xju.sem.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.social.dto.ConversationDTO;
import com.xju.sem.module.social.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * message 的 MyBatis-Plus Mapper。自定义查询均手动带 {@code deleted = 0}
 * （自定义 @Select/@Update 不经过 wrapper，不会被全局逻辑删除配置自动改写）。
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 会话列表：当前用户参与的每个会话（对端 = 另一方），各返回最后一条消息内容/时间 + 未读数，
     * 按最后消息时间倒序。peer_id 先在内层子查询按 IF(sender_id=userId, receiver_id, sender_id)
     * 聚合出每个对端及其 MAX(created_at)，外层再用相关子查询取该对端会话的最后一条内容与未读数，
     * 避免 GROUP BY 下非聚合列取值不确定的问题。peerName 本期不查用户表，留空由前端兜底展示。
     */
    @Select("SELECT "
            + "  p.peer_id AS peerId, "
            + "  NULL AS peerName, "
            + "  ( "
            + "    SELECT m2.content FROM message m2 "
            + "    WHERE m2.deleted = 0 "
            + "      AND ((m2.sender_id = #{userId} AND m2.receiver_id = p.peer_id) "
            + "        OR (m2.sender_id = p.peer_id AND m2.receiver_id = #{userId})) "
            + "    ORDER BY m2.created_at DESC, m2.id DESC LIMIT 1 "
            + "  ) AS lastContent, "
            + "  p.last_at AS lastAt, "
            + "  ( "
            + "    SELECT COUNT(*) FROM message m3 "
            + "    WHERE m3.deleted = 0 AND m3.receiver_id = #{userId} "
            + "      AND m3.sender_id = p.peer_id AND m3.is_read = 0 "
            + "  ) AS unreadCount "
            + "FROM ( "
            + "  SELECT IF(sender_id = #{userId}, receiver_id, sender_id) AS peer_id, "
            + "         MAX(created_at) AS last_at "
            + "  FROM message "
            + "  WHERE deleted = 0 AND (sender_id = #{userId} OR receiver_id = #{userId}) "
            + "  GROUP BY IF(sender_id = #{userId}, receiver_id, sender_id) "
            + ") p "
            + "ORDER BY p.last_at DESC")
    List<ConversationDTO> listConversations(@Param("userId") Long userId);

    /** 会话历史：与指定对端之间的全部消息，按时间正序。 */
    @Select("SELECT * FROM message "
            + "WHERE ((sender_id = #{userId} AND receiver_id = #{peerId}) "
            + "    OR (sender_id = #{peerId} AND receiver_id = #{userId})) "
            + "  AND deleted = 0 "
            + "ORDER BY created_at ASC")
    List<Message> listHistory(@Param("userId") Long userId, @Param("peerId") Long peerId);

    /** 将指定对端发给当前用户的未读消息标记为已读。 */
    @Update("UPDATE message SET is_read = 1 "
            + "WHERE receiver_id = #{userId} AND sender_id = #{peerId} "
            + "  AND is_read = 0 AND deleted = 0")
    int markRead(@Param("userId") Long userId, @Param("peerId") Long peerId);

    /** 当前用户全部会话的未读消息总数。 */
    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{userId} AND is_read = 0 AND deleted = 0")
    long countUnread(@Param("userId") Long userId);
}
