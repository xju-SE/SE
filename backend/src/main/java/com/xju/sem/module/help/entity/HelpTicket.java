package com.xju.sem.module.help.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 求助单（对应 schema.sql {@code help_ticket} 表，精确列以该表为准）。
 * id/deleted/createdAt/updatedAt 继承自 {@link BaseEntity}。
 *
 * <p>状态机 status：OPEN→MATCHED→ANSWERED→ADOPTED→CLOSED，取值枚举见
 * {@link com.xju.sem.module.help.enums.HelpTicketStatus}，按地基约定用 String 存储、不引入
 * MyBatis 枚举 TypeHandler，保持与标签/枚举数据解耦。
 *
 * <p>与 04 详细设计的差异（以 schema.sql 为准的精简，详见实现说明"假设与简化"一节）：
 * 本表无 version 乐观锁列——状态流转改用"带 WHERE status=? 条件的 CAS UPDATE"实现（见
 * {@link com.xju.sem.module.help.mapper.HelpTicketMapper}），并发下后写者受影响行数为 0 即判冲突，
 * 效果等价乐观锁；无 close_reason / adopted_answer_id / view_count / 角色快照列，采纳回答通过
 * help_answer.is_adopted=1 唯一定位。target_direction 为自由文本（VARCHAR），非标签外键。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("help_ticket")
public class HelpTicket extends BaseEntity {

    /** 发布人（求助人）user.id。 */
    private Long askerId;

    /** 标题。 */
    private String title;

    /** 问题详情。 */
    private String content;

    /** 相关专业标签 FK→tag.id (tag_type=MAJOR)，发布时从发布人档案只读快照写入。 */
    private Long majorTagId;

    /** 提问时年级档 1..10；ALUMNI/ADMIN 发布时为 NULL（无在读年级，不参与"学长年级差"打分）。 */
    private Integer gradeLevel;

    /** 问题类型标签 FK→tag.id (tag_type=QUESTION_TYPE)。 */
    private Long questionTypeTagId;

    /** 目标方向（考研/就业/竞赛...），自由文本，可空。 */
    private String targetDirection;

    /** OPEN/MATCHED/ANSWERED/ADOPTED/CLOSED，见 {@link com.xju.sem.module.help.enums.HelpTicketStatus}。 */
    private String status;

    /** 追问计数（限次追问口径：仅求助人追问累加，见 §6.3 与实现说明）。 */
    private Integer followupCount;
}
