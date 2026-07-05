package com.xju.sem.module.admin.service;

import com.xju.sem.module.admin.dto.ApplyContributorCertRequest;
import com.xju.sem.module.admin.dto.AuditTaskDTO;

/** 贡献者认证申请（FR-M7-18，07 详细设计 §8："内部委托 AuditTaskService.createTask"）。 */
public interface ContributorCertService {

    /**
     * 已认证 ALUMNI 提交贡献者认证申请：校验角色/认证状态与"无待处理申请中"，创建
     * {@code audit_task}(target_type=CONTRIBUTOR_CERT, target_id=alumniUserId, status=PENDING)，
     * 申请材料（honorCertUrl/note）落 {@code auto_precheck} 列（见 {@code ContributorCertPayload}）。
     */
    AuditTaskDTO apply(Long alumniUserId, ApplyContributorCertRequest request);
}
