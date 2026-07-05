package com.xju.sem.module.timeline.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 下线模板（FR-M6-02）。reason 记入操作原因（本期仅透传，不落审计表）。 */
@Data
public class OfflineTemplateRequest {

    @Size(max = 300, message = "下线原因长度不能超过300字")
    private String reason;
}
