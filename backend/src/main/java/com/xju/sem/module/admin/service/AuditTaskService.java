package com.xju.sem.module.admin.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.admin.dto.AuditTaskBriefDTO;
import com.xju.sem.module.admin.dto.AuditTaskDTO;
import com.xju.sem.module.admin.dto.AuditTaskDetailDTO;
import com.xju.sem.module.admin.dto.AuditTaskQuery;
import com.xju.sem.module.admin.dto.BatchResultDTO;
import com.xju.sem.module.admin.dto.ChecklistResult;
import com.xju.sem.module.admin.enums.AuditDecision;

import java.util.List;
import java.util.Map;

/**
 * 统一审核任务 Service（M7 审核队列范围，07 详细设计 §8 契约的精简实现）。
 */
public interface AuditTaskService {

    /** 供 {@code AuditTaskEventListener} 内部调用：目标模块提交审核事件 → 建 PENDING 任务。 */
    AuditTaskDTO createTask(String targetType, Long targetId, Long submitterId, String reviewKind);

    /** 供 M1 自动初审（SSO/邀请码）通过留痕调用：直接落 AUTO_APPROVED，不进入人工队列。 */
    AuditTaskDTO recordAutoApproved(String targetType, Long targetId, Long submitterId, String decisionNote);

    /** FR-M7-05：对指定任务关联的知识候选跑自动预检并写回 audit_task.auto_precheck。 */
    void runPreCheck(Long taskId, Long knowledgeEntryId);

    /** FR-M7-01 统一审核队列列表；query.status 为空时按 PENDING 处理。 */
    PageResult<AuditTaskBriefDTO> pageQueue(AuditTaskQuery query);

    /** 各 target_type 当前 PENDING 待处理数小计，供队列顶部徽标。 */
    Map<String, Long> countPendingByType();

    /** FR-M7-02 审核任务详情。 */
    AuditTaskDetailDTO getById(Long id);

    /**
     * FR-M7-03/04：单条终审决定。KNOWLEDGE_ENTRY 且 checklistResult 三项任一勾选时，强制将
     * decision 转为 RETURN（忽略传入值），见 07 详细设计 §6.3。audit_task 状态 CAS 更新与目标
     * 模块 Service 调用在同一物理事务内完成，二者要么同时提交要么同时回滚（§6.4）。
     */
    AuditTaskDTO decide(Long id, Long reviewerId, AuditDecision decision, ChecklistResult checklistResult,
                        String reasonCode, String comment);

    /** FR-M7-06/07 批量通过/退回：仅 supportsBatch()=true 的 targetType 允许，逐条独立事务，单条失败不影响其余。 */
    BatchResultDTO batchDecide(String targetType, List<Long> ids, Long reviewerId, AuditDecision decision,
                               String reasonCode, String comment);
}
