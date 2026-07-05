package com.xju.sem.module.admin.mapper;

import lombok.Data;

/** {@link AuditTaskMapper#countByTargetTypeGroupStatus(String)} 的分组计数投影行，非持久化实体。 */
@Data
public class StatusCount {
    private String status;
    private Long cnt;
}
