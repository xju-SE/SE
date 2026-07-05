package com.xju.sem.module.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 审核吞吐量趋势（FR-M7-20，07 详细设计 §6.5 PDAT 峰值对照）。 */
@Data
@Builder
public class AuditThroughputStatsDTO {
    private LocalDate dateFrom;
    private LocalDate dateTo;

    /** 按日已决策数（APPROVED/RETURNED/REJECTED，不含 AUTO_APPROVED），按日期升序。 */
    private List<DailyDecidedCountDTO> dailyDecided;

    /** §6.5 公式估算的"高峰期最大日审核吞吐量"（PDAT），供折线图叠加参考线。 */
    private double peakDailyThroughputEstimate;
}
