package com.xju.sem.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PATCH /api/v1/reports/{id}/handle 请求体（FR-M7-11）。 */
@Data
public class HandleReportRequest {

    @NotBlank(message = "decision 不能为空")
    private String decision;

    /** UPHELD 时必填：NONE/CONTENT_HIDDEN/CONTENT_OFFLINE/USER_DISABLED，须与 targetType 匹配。 */
    private String handleAction;

    @Size(max = 280, message = "处理说明不能超过280字")
    private String handleComment;
}
