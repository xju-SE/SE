package com.xju.sem.module.admin.dto;

import lombok.Data;

import java.util.List;

/** PATCH /api/v1/audit-tasks/batch-decide 请求体（FR-M7-06/07）。 */
@Data
public class BatchDecideRequest {
    private String targetType;
    private List<Long> ids;
    private String decision;
    private String reasonCode;
    private String comment;
}
