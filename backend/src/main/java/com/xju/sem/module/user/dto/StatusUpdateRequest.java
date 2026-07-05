package com.xju.sem.module.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** ADMIN 启用/禁用账号入参。 */
@Data
public class StatusUpdateRequest {

    @Pattern(regexp = "ACTIVE|DISABLED", message = "status 只能是 ACTIVE 或 DISABLED")
    private String status;

    private String reason;
}
