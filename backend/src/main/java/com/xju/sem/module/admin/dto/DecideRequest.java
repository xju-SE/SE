package com.xju.sem.module.admin.dto;

import lombok.Data;

/** PATCH /api/v1/audit-tasks/{id}/decide 请求体。 */
@Data
public class DecideRequest {

    /** APPROVE/RETURN/REJECT。 */
    private String decision;

    /** 标准理由模板编码（见 ReasonTemplate），可选。 */
    private String reasonCode;

    /** 审核意见补充说明，可选（checklist 命中且未填时自动落标准理由文案）。 */
    private String comment;

    /** 仅 KNOWLEDGE_ENTRY 使用的隐私 checklist 勾选结果。 */
    private ChecklistResult checklistResult;
}
