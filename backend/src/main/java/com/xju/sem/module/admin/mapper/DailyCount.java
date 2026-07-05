package com.xju.sem.module.admin.mapper;

import lombok.Data;

import java.time.LocalDate;

/** {@link AuditTaskMapper#dailyDecidedCounts} 的按日分组计数投影行，非持久化实体。 */
@Data
public class DailyCount {
    private LocalDate day;
    private Long cnt;
}
