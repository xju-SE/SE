package com.xju.sem.module.opportunity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机会摘要出参。同时承担两个角色：
 * 1) 本模块 list（P13 列表/组队广场反查）行数据；
 * 2) 跨模块契约 {@code OpportunityService.getBrief(Long id)} 的返回类型（供 M6 时间线节点引用、
 *    首页仪表盘"即将截止机会"卡片只读引用），类名/字段与地基契约保持一致。
 */
@Data
@Builder
public class OpportunityBriefDTO {

    private Long id;
    private String type;
    private String title;
    private String status;
    private LocalDateTime deadline;
    private Long publisherId;
    private String publisherName;
    private Boolean isReferral;

    /** S19：是否允许围绕本机会发起组队（team_required=1 时前端才展示"发起队伍"入口）。 */
    private Boolean teamRequired;
    private LocalDateTime createdAt;
}
