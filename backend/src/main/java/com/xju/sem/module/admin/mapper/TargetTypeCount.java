package com.xju.sem.module.admin.mapper;

import lombok.Data;

/** {@link AuditTaskMapper#countPendingByType()} 的分组计数投影行，非持久化实体。 */
@Data
public class TargetTypeCount {
    private String targetType;
    private Long cnt;
}
