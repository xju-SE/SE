package com.xju.sem.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * notification 的 MyBatis-Plus Mapper。列表分页走 {@code selectPage}（wrapper 级查询，全局逻辑删除
 * 配置自动生效）；已读态流转与未读计数为自定义 SQL，需手动带 {@code deleted = 0}（自定义 @Select/@Update
 * 不经过 wrapper，不会被全局逻辑删除配置自动改写，与 AuditTaskMapper/HelpTicketMapper 同一处理方式）。
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 标记单条已读，同时校验归属（WHERE id=? AND user_id=?），防止越权标记他人通知。
     * 若该行本就是已读（is_read=1），MySQL 对"值未变化"的 UPDATE 也可能返回 0 影响行——
     * Service 层据此对 0 行做二次 selectById 以区分"已是已读"（幂等成功）与"不存在/无权限"。
     */
    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    /** 全部已读：仅影响当前未读的行，返回影响行数供日志/统计使用。 */
    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    int markAllRead(@Param("userId") Long userId);

    /** 未读数徽标（P17 顶部角标）。 */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    long countUnread(@Param("userId") Long userId);
}
