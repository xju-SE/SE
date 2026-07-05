package com.xju.sem.module.help.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回答出参（三段式模板）。responderRole/responderName 供前端渲染"校友/学长学姐"徽标。
 */
@Data
public class HelpAnswerDTO {

    private Long id;
    private Long ticketId;
    private Long responderId;
    private String responderName;
    /** 回答人注册身份：STUDENT/ALUMNI/ADMIN。 */
    private String responderRole;
    private String precondition;
    private List<String> steps;
    private String cautions;
    private Integer isAdopted;
    /** 采纳后生成的知识候选 id（回写后才有值）。 */
    private Long knowledgeEntryId;
    private LocalDateTime createdAt;
}
