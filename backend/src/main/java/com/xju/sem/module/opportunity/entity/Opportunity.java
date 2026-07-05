package com.xju.sem.module.opportunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 机会（表 opportunity）。类型化信息聚合 + 以 deadline 驱动的状态机（见 {@code OpportunityStatus}）。
 * 状态类流转统一走状态 CAS（{@code UPDATE ... WHERE status=期望前置值}），schema 无 version 列，
 * 与 M1 auth_application、M3/M4 审核/状态类操作同一分工原则。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opportunity")
public class Opportunity extends BaseEntity {

    /** COMPETITION/INNOVATION/INTERNSHIP/LECTURE。 */
    private String type;

    private String title;

    private String description;

    /** 报名截止时间，驱动状态机的核心字段。 */
    private LocalDateTime deadline;

    /** PENDING_REVIEW/ONGOING/CLOSING_SOON/CLOSED/ENDED，见 {@code OpportunityStatus}。 */
    private String status;

    /** 发布人（ALUMNI 或 ADMIN，已认证）。 */
    private Long publisherId;

    /** 是否内推类：1 时需 M7 终审通过方可对外公开（TINYINT，0/1）。 */
    private Integer isReferral;

    /** S19：是否允许围绕本机会发起组队，1 才可 {@code createTeam} 关联本机会（TINYINT，0/1）。 */
    private Integer teamRequired;
}
