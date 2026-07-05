package com.xju.sem.module.opportunity.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标记结束/解散队伍入参（FR-M5-17③）。reason 为可选说明文本——schema 的 team 无 end_reason 列，
 * 故本期仅随通知下发+落日志留痕、不入库（见实现说明"假设与简化"）。
 */
@Data
public class EndTeamRequest {

    @Size(max = 200, message = "结束原因不能超过200字")
    private String reason;
}
