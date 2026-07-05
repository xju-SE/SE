package com.xju.sem.module.help.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 追问（对应 schema.sql {@code help_followup} 表）。
 *
 * <p>该表结构较精简，仅有 ticket_id/from_user_id/content/deleted/created_at（无 updated_at、
 * 无 target_answer_id、无 sender_role），故本实体不继承 {@link com.xju.sem.common.BaseEntity}
 * （后者含 updatedAt 字段，会向不存在的列写值）。追问按求助单（而非按具体回答）线程组织，
 * 与 04 设计文档的按回答分线程相比是随 schema 的简化，见实现说明"假设与简化"。
 */
@Data
@TableName("help_followup")
public class HelpFollowup implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属求助单 help_ticket.id。 */
    private Long ticketId;

    /** 发送人 user.id（求助人追问 或 回答人回复）。 */
    private Long fromUserId;

    /** 追问/回复内容。 */
    private String content;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    /** 创建时间，仅插入填充（本表无 updated_at 列）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
