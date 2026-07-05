package com.xju.sem.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /api/v1/reports 请求体（FR-M7-09）。 */
@Data
public class SubmitReportRequest {

    @NotBlank(message = "targetType 不能为空")
    private String targetType;

    @NotNull(message = "targetId 不能为空")
    private Long targetId;

    @NotBlank(message = "reasonType 不能为空")
    private String reasonType;

    @Size(max = 280, message = "举报说明不能超过280字")
    private String description;
}
