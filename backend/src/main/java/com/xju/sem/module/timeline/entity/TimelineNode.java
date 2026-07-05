package com.xju.sem.module.timeline.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 时间线节点（表 timeline_node）。含逻辑删除与审计时间（继承 {@link BaseEntity}）。
 *
 * <p>{@code suggestedTime} 为人类可读展示串（如"大一上第 8 周"），仅透传展示；{@code suggestedMonth}
 * 为机读月份（1-12），配合 {@code stage} 与用户 enrollYear 换算绝对建议年月并做逾期比对（§6.4）。
 * {@code importance} 为 TINYINT 1-3（越大越关键），用作 §6.5 补救优先级打分的权重维度。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("timeline_node")
public class TimelineNode extends BaseEntity {

    /** 所属模板 id。 */
    private Long templateId;

    /** 节点标题。 */
    private String title;

    /** 学期阶段枚举 GRADE1_1..GRADE4_2，见 {@code Stage}。 */
    private String stage;

    /** 建议完成时间展示串（相对，如"大一上第 8 周"），仅展示、不参与逾期计算。 */
    private String suggestedTime;

    /** 建议完成月份 1-12，供逾期比对（§6.4）。 */
    private Integer suggestedMonth;

    /** 重要度 1-3（越大越关键），补救优先级权重维度（§6.5）。 */
    private Integer importance;

    /** 同一 stage 内展示顺序，越小越靠前。 */
    private Integer orderNo;

    /** 行动指引说明（节点自身文案，非对 M2/M3/M5 内容的复制）。 */
    private String description;
}
