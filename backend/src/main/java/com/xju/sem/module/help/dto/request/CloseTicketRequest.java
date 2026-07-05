package com.xju.sem.module.help.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 关闭求助单入参（FR-M4-12）。closeReason 为可选说明文本——schema 的 help_ticket 无 close_reason 列，
 * 故本期仅落日志留痕、不入库（见实现说明"假设与简化"）。
 */
@Data
public class CloseTicketRequest {

    @Size(max = 50, message = "关闭原因不能超过50字")
    private String closeReason;
}
