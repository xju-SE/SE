package com.xju.sem.module.opportunity.dto.request;

import lombok.Data;

/** 组队广场列表查询条件（FR-M5-18）。默认仅展示 RECRUITING（status 为空时由 Service 兜底）。 */
@Data
public class TeamQuery {

    private Long opportunityId;

    /** RECRUITING/FULL/ONGOING/ENDED，可空（默认仅 RECRUITING）。 */
    private String status;

    private String keyword;

    private Integer page;

    private Integer size;
}
