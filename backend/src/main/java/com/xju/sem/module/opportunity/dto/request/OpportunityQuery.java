package com.xju.sem.module.opportunity.dto.request;

import lombok.Data;

/** 机会列表查询条件（FR-M5-06：类型筛选/即将截止）。viewerUserId 由 Controller 从登录态注入，不来自客户端。 */
@Data
public class OpportunityQuery {

    /** COMPETITION/INNOVATION/INTERNSHIP/LECTURE，可空。 */
    private String type;

    /** true 时仅返回 CLOSING_SOON（"即将截止"专区，P03 首页仪表盘复用）。 */
    private boolean closingSoon;

    /** 默认 false：排除 CLOSED/ENDED；true 时"显示已截止/已结束"。 */
    private boolean includeEnded;

    /** 标题关键字，长度 ≤200。 */
    private String keyword;

    private Integer page;

    private Integer size;
}
