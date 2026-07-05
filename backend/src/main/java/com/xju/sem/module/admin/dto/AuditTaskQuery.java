package com.xju.sem.module.admin.dto;

import lombok.Data;

/** FR-M7-01 统一审核队列分页查询条件。status 缺省在 Service 内按 PENDING 处理。 */
@Data
public class AuditTaskQuery {
    private String targetType;
    private String status;
    private String keyword;
    private int page = 1;
    private int size = 10;
}
