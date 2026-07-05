package com.xju.sem.module.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 统一审核队列列表行（P18 Tab① 表格行）。 */
@Data
@Builder
public class AuditTaskBriefDTO {
    private Long id;
    private String targetType;
    private Long targetId;
    private String reviewKind;
    private String status;
    private Long submitterId;

    /** 提交人展示名（真实姓名优先，取不到则用户名），跨模块只读拼装，解析失败时为 null。 */
    private String submitterName;

    /** 目标内容摘要（认证：申请角色+姓名/学号；知识候选：标题），跨模块只读拼装，解析失败时为 null。 */
    private String targetSummary;

    /** 仅 KNOWLEDGE_ENTRY：auto_precheck 命中疑似联系方式/身份证号时为 true，供前端红色警示图标。 */
    private boolean privacyAlert;

    private LocalDateTime createdAt;
}
