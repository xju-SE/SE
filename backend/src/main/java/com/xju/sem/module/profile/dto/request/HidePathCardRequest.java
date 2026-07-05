package com.xju.sem.module.profile.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报下架入参（PATCH /alumni-path-cards/{id}/hide，FR-M2-11，供 M7 治理调用）。
 */
@Data
public class HidePathCardRequest {

    /** 下架原因（记录到日志/审计，便于复核）。 */
    @Size(max = 255, message = "原因过长")
    private String reason;
}
