package com.xju.sem.module.help.dto.response;

import lombok.Data;

/**
 * 列表页顶部统计卡（FR-M4-04，仪表盘化）：本专业待解决数 / 已解决数 / 平均响应时长。
 */
@Data
public class TicketStatCardDTO {

    /** 本专业待解决求助数（status∈OPEN/MATCHED/ANSWERED）。 */
    private Long openCount;

    /** 本专业已解决数（status∈ADOPTED/CLOSED）。 */
    private Long resolvedCount;

    /** 平均响应时长（小时）：求助创建到首条回答；无样本时为 null。 */
    private Double avgResponseHours;
}
