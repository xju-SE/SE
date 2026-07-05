package com.xju.sem.module.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 举报出参（FR-M7-09/10/11/12）。 */
@Data
@Builder
public class ReportDTO {
    private Long id;
    private String targetType;
    private Long targetId;

    /** 目标内容摘要（复用对应模块 getBrief/getById 只读拼装），解析失败时为 null。 */
    private String targetSummary;

    private Long reporterId;
    private String reporterName;

    /** PRIVACY_LEAK/FALSE_INFO/INAPPROPRIATE_CONTENT/SPAM_AD/OTHER，从 reason 折叠文本解析。 */
    private String reasonType;

    private String description;

    /** PENDING/HANDLED/DISMISSED。 */
    private String status;

    private Long handlerId;

    /** NONE/CONTENT_HIDDEN/CONTENT_OFFLINE/USER_DISABLED，从 handle_note 折叠文本解析。 */
    private String handleAction;

    private String handleComment;

    private LocalDateTime createdAt;

    /** 近似处理时间：非 PENDING 时取 updated_at（schema 无独立 handled_at 列，见实现说明）。 */
    private LocalDateTime handledAt;
}
