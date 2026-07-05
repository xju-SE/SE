package com.xju.sem.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/** 单日已决策审核任务数（供 §6.5 审核吞吐量趋势图一个数据点）。 */
@Data
@AllArgsConstructor
public class DailyDecidedCountDTO {
    private LocalDate date;
    private long count;
}
