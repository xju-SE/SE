package com.xju.sem.module.help.dto.request;

import lombok.Data;

/**
 * 求助单列表查询条件（FR-M4-04，本专业高频仪表盘化，非信息流）。
 * majorTagId 为空时由 Service 默认取当前用户本人专业。
 */
@Data
public class HelpTicketQuery {

    /** 专业过滤，默认当前用户 major_tag_id。 */
    private Long majorTagId;

    /** 问题类型标签过滤，可空。 */
    private Long questionTypeTagId;

    /** 状态过滤（OPEN/MATCHED/ANSWERED/ADOPTED/CLOSED），可空。 */
    private String status;

    /** 排序方式：LATEST(默认) / NEARLY_TIMEOUT(最久未处理优先) / UNANSWERED_FIRST(无人回应优先)。 */
    private String sortBy;

    private Integer page;

    private Integer size;
}
