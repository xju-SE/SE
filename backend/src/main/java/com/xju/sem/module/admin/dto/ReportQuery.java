package com.xju.sem.module.admin.dto;

import lombok.Data;

/** FR-M7-10 举报队列（治理端）查询条件。status 缺省时按 PENDING 处理。 */
@Data
public class ReportQuery {
    private String status;
    private String targetType;
    private int page = 1;
    private int size = 10;
}
