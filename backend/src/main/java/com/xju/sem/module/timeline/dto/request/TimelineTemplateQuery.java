package com.xju.sem.module.timeline.dto.request;

import lombok.Data;

/** 模板列表查询条件（ADMIN，FR-M6-01 附带列表）。全部可空，为空则不按该维度过滤。 */
@Data
public class TimelineTemplateQuery {

    private Long majorTagId;

    private String routeType;

    /** DRAFT/PUBLISHED/OFFLINE，可空。 */
    private String status;

    private Integer page;

    private Integer size;
}
