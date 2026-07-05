package com.xju.sem.module.help.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 求助单出参（列表行 / 详情头）。不直接返回 entity，避免把持久化对象当出参。
 */
@Data
public class HelpTicketDTO {

    private Long id;
    private Long askerId;
    /** 求助人姓名摘要（跨模块 UserService.getBrief 提供）。 */
    private String askerName;
    private String title;
    private String content;
    private Long majorTagId;
    private Integer gradeLevel;
    private Long questionTypeTagId;
    private String targetDirection;
    private String status;
    private Integer followupCount;
    /** 回答条数（列表/详情聚合）。 */
    private Integer answerCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
