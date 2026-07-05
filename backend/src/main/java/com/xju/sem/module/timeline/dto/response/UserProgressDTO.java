package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 个人进度标记出参（FR-M6-08 返回）。 */
@Data
@Builder
public class UserProgressDTO {

    private Long nodeId;
    /** NOT_STARTED/DONE。 */
    private String status;
    private LocalDateTime markedAt;
}
