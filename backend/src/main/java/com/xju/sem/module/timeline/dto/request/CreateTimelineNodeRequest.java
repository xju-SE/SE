package com.xju.sem.module.timeline.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增时间线节点（FR-M6-03）。stage 合法性、suggestedMonth∈[1,12]、importance∈[1,3]、orderNo≥0
 * 在 Service 层校验。suggestedTime 为展示串（可空）；suggestedMonth 供逾期比对（可空，空则该节点
 * 不参与逾期判定，仅展示）。
 */
@Data
public class CreateTimelineNodeRequest {

    @NotBlank(message = "阶段不能为空")
    private String stage;

    @NotBlank(message = "节点标题不能为空")
    @Size(max = 150, message = "节点标题长度不能超过150字")
    private String title;

    @Size(max = 500, message = "节点说明长度不能超过500字")
    private String description;

    /** 建议完成时间展示串（如"大一上第 8 周"），可空。 */
    @Size(max = 50, message = "建议时间展示串长度不能超过50字")
    private String suggestedTime;

    /** 建议完成月份 1-12，可空（空则不参与逾期比对）。 */
    private Integer suggestedMonth;

    /** 重要度 1-3（越大越关键），默认 1。 */
    private Integer importance;

    /** 同一 stage 内展示顺序，默认 0。 */
    private Integer orderNo;
}
