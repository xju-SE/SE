package com.xju.sem.module.timeline.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 编辑时间线节点（FR-M6-03）。字段语义同新增；template 归属为节点身份键，不在编辑接口变更。 */
@Data
public class UpdateTimelineNodeRequest {

    @NotBlank(message = "阶段不能为空")
    private String stage;

    @NotBlank(message = "节点标题不能为空")
    @Size(max = 150, message = "节点标题长度不能超过150字")
    private String title;

    @Size(max = 500, message = "节点说明长度不能超过500字")
    private String description;

    @Size(max = 50, message = "建议时间展示串长度不能超过50字")
    private String suggestedTime;

    private Integer suggestedMonth;

    private Integer importance;

    private Integer orderNo;
}
