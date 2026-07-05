package com.xju.sem.module.timeline.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 标记/切换节点个人进度（FR-M6-08）。status∈{NOT_STARTED,DONE}，合法性在 Service 校验。 */
@Data
public class MarkProgressRequest {

    @NotBlank(message = "进度状态不能为空")
    private String status;
}
