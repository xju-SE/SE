package com.xju.sem.module.timeline.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 编辑模板基本信息（FR-M6-01）。专业/路线为模板身份键，不在编辑接口变更；仅改展示名称。 */
@Data
public class UpdateTimelineTemplateRequest {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100字")
    private String name;
}
