package com.xju.sem.module.timeline.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 时间线模板（表 timeline_template，专业 × 路线）。含逻辑删除与审计时间（继承 {@link BaseEntity}）。
 *
 * <p>{@code majorTagId} 为 NULL 表示"全专业通用模板"，专供 UNDECIDED 默认线及分化路线未逐专业
 * 定制时的兜底（解析算法 §6.2）。发布态 {@code status} 走状态 CAS（schema 无 version 列），
 * 与 M5 opportunity 同一并发分工。schema 未设 description 列，模板简介合并进不落库的展示说明，
 * 本实体不含该字段（相对详细设计的裁剪，见实现说明"假设与简化"）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("timeline_template")
public class TimelineTemplate extends BaseEntity {

    /** 专业标签（tag.tag_type=MAJOR）；NULL=全专业通用模板。 */
    private Long majorTagId;

    /** UNDECIDED/POSTGRAD/EMPLOY/COMPETITION/CIVIL，见 {@code RouteType}。 */
    private String routeType;

    /** 模板名称，如"计算机科学与技术—考研路线"。 */
    private String name;

    /** DRAFT/PUBLISHED/OFFLINE，见 {@code TemplateStatus} 与 §4.1。 */
    private String status;

    /** 创建/维护的 ADMIN 用户 id。 */
    private Long createdBy;
}
