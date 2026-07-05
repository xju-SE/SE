package com.xju.sem.module.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 单条审核任务出参（decide 的返回体）。 */
@Data
@Builder
public class AuditTaskDTO {
    private Long id;
    private String targetType;
    private Long targetId;
    private Long submitterId;
    private String reviewKind;
    private String status;
    private Long reviewerId;
    private String decisionNote;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
