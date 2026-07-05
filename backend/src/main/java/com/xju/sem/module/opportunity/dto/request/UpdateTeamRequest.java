package com.xju.sem.module.opportunity.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 编辑队伍信息（FR-M5-12）。capacity 不可小于当前已批准人数，见 Service 层校验。 */
@Data
public class UpdateTeamRequest {

    @NotBlank(message = "队伍名称不能为空")
    @Size(max = 100, message = "队伍名称不能超过100字")
    private String title;

    @Size(max = 1000, message = "招募说明过长")
    private String description;

    @Size(max = 500, message = "招募需求描述不能超过500字")
    private String needDesc;

    @NotNull(message = "人数上限不能为空")
    @Min(value = 2, message = "人数上限至少为2")
    @Max(value = 20, message = "人数上限最多为20")
    private Integer capacity;
}
