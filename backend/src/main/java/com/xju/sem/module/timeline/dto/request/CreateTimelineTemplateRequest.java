package com.xju.sem.module.timeline.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建时间线模板（FR-M6-01，默认 DRAFT）。majorTagId 可空=全专业通用模板；routeType 合法性在 Service 校验。 */
@Data
public class CreateTimelineTemplateRequest {

    /** 专业标签 id，可空（空=全专业通用模板）。 */
    private Long majorTagId;

    @NotBlank(message = "路线类型不能为空")
    private String routeType;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100字")
    private String name;
}
