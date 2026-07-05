package com.xju.sem.module.admin.service;

import com.xju.sem.module.admin.dto.AuditThroughputStatsDTO;
import com.xju.sem.module.admin.dto.OperationOverviewDTO;

import java.time.LocalDate;

/** 运营数据统计看板（FR-M7-20/21，07 详细设计 §6.5/§6.6）。 */
public interface OperationStatsService {

    /** dateFrom/dateTo 为空时默认近 7 日。 */
    OperationOverviewDTO getOverview(LocalDate from, LocalDate to);

    /** dateFrom/dateTo 为空时默认近 7 日；含 §6.5 PDAT 估算值供参考线对照。 */
    AuditThroughputStatsDTO getAuditThroughput(LocalDate from, LocalDate to);
}
