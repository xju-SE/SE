package com.xju.sem.module.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * GET /api/v1/audit-tasks 响应体：{@code {records, total, page, size, countByType}}
 * （07 详细设计 §5(b)）。countByType 为各 target_type 当前 PENDING 待处理数小计，供顶部徽标展示，
 * 与本次查询的 status/targetType 筛选条件无关（始终反映全量待处理积压）。
 */
@Data
@Builder
public class AuditQueueResponse {
    private List<AuditTaskBriefDTO> records;
    private long total;
    private long page;
    private long size;
    private Map<String, Long> countByType;
}
