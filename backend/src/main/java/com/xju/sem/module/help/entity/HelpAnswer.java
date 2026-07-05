package com.xju.sem.module.help.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 回答（三段式模板：前提 / 步骤 / 注意事项），对应 schema.sql {@code help_answer} 表。
 *
 * <p>steps 列为 VARCHAR(2000)，本实现以 JSON 数组文本存储有序步骤，实体侧用
 * {@link JacksonTypeHandler} 自动映射 {@code List<String>} ↔ JSON（需 {@code autoResultMap=true}），
 * Controller 层直接收发结构化数组；供 M3 的 {@code AnswerContentDTO.steps} 出参时再渲染为带序号的纯文本。
 *
 * <p>唯一约束：schema 未落 uk_ticket_responder，"同一人对同一求助单只保留一条回答"由 Service 层
 * 应用级校验保证（见 {@link com.xju.sem.module.help.service.impl.HelpAnswerServiceImpl}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "help_answer", autoResultMap = true)
public class HelpAnswer extends BaseEntity {

    /** 所属求助单 help_ticket.id。 */
    private Long ticketId;

    /** 回答人 user.id。 */
    private Long responderId;

    /** 【三段式-前提】适用前提/背景条件，可空。 */
    private String precondition;

    /** 【三段式-步骤】有序步骤数组，JSON 文本落库。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> steps;

    /** 【三段式-注意事项】可空。 */
    private String cautions;

    /** 是否被采纳 0/1；同一 ticket_id 下至多一条为 1。 */
    private Integer isAdopted;

    /** 采纳后由 M3 生成的知识候选 id，AFTER_COMMIT 监听器回写（链1补列）。 */
    private Long knowledgeEntryId;
}
