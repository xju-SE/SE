package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时间线模板出参。同时是跨内部服务契约 {@code TimelineTemplateService.resolve(...)} 的返回类型
 * （§6.2 解析结果，供 UserProgressService 取 templateId 与 routeType）。
 */
@Data
@Builder
public class TimelineTemplateDTO {

    private Long id;
    private Long majorTagId;
    private String routeType;
    private String name;
    /** DRAFT/PUBLISHED/OFFLINE。 */
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
