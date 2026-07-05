package com.xju.sem.module.knowledge.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手动下线（FR-M3-07）。reason 仅用于通知文案与操作日志，knowledge_entry 表无专门列持久化它
 * （若为举报处理场景，举报理由已由 M7 report.reason 承载，不在本表重复存储）。
 */
@Data
public class OfflineRequest {

    @Size(max = 300, message = "下线理由不能超过300字")
    private String reason;
}
