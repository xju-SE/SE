package com.xju.sem.module.timeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 个人节点进度（表 user_progress）。同一用户对同一节点仅一条记录（uk_user_node），upsert 语义。
 *
 * <p><b>不继承 {@link com.xju.sem.common.BaseEntity}</b>：schema.sql 该表仅 {@code (id, user_id,
 * node_id, status, marked_at)} 五列，无 {@code deleted/created_at/updated_at}。因"同一行只有本人
 * 一个写者"，并发上直接用 {@code INSERT ... ON DUPLICATE KEY UPDATE}，不需乐观锁/状态 CAS。
 * {@code markedAt} 在标记 DONE 时写入、回切 NOT_STARTED 时清空。
 */
@Data
@TableName("user_progress")
public class UserProgress implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 id（STUDENT）。 */
    private Long userId;

    /** 对应节点 id。 */
    private Long nodeId;

    /** NOT_STARTED/DONE，见 {@code ProgressStatus} 与 §4.2。 */
    private String status;

    /** 标记完成时间；回切 NOT_STARTED 时清空。 */
    private LocalDateTime markedAt;
}
