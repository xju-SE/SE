package com.xju.sem.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 贡献者认证申请材料（FR-M7-18）。schema.sql 的 {@code audit_task} 无 {@code payload} 列（07 详细
 * 设计 §3.1 设想的通用附加数据列在本次精简实现中未落地），故复用 {@code auto_precheck}
 * （VARCHAR(500)，本为 KNOWLEDGE_ENTRY 预检结果预留）序列化落库——与 {@code decision_note}
 * 折叠"理由码+补充说明"同一处理思路：一个通用文本列按 target_type 语义复用，而非新增列。
 * 序列化/反序列化见 {@code ContributorCertServiceImpl}/{@code ContributorCertAuditHandler}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContributorCertPayload {

    /** 荣誉证明附件 URL。 */
    private String honorCertUrl;

    /** 申请说明，可空。 */
    private String note;
}
