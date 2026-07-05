package com.xju.sem.module.opportunity.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手动结束/强制下线机会入参（FR-M5-04/05）。reason 为可选说明文本——schema 的 opportunity 无
 * end_reason/review_comment 列，故本期仅随通知下发+落日志留痕、不入库（见实现说明"假设与简化"）。
 */
@Data
public class EndOpportunityRequest {

    @Size(max = 200, message = "结束原因不能超过200字")
    private String reason;
}
