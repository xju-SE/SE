package com.xju.sem.module.admin.controller;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.admin.dto.AuditQueueResponse;
import com.xju.sem.module.admin.dto.AuditTaskBriefDTO;
import com.xju.sem.module.admin.dto.AuditTaskDTO;
import com.xju.sem.module.admin.dto.AuditTaskDetailDTO;
import com.xju.sem.module.admin.dto.AuditTaskQuery;
import com.xju.sem.module.admin.dto.BatchDecideRequest;
import com.xju.sem.module.admin.dto.BatchResultDTO;
import com.xju.sem.module.admin.dto.DecideRequest;
import com.xju.sem.module.admin.enums.AuditDecision;
import com.xju.sem.module.admin.service.AuditTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统一审核队列 Controller（P18 Tab①，07 详细设计 §5(b) FR-M7-01/02/03/04/06/07）。
 * 仅做入参校验转发，业务逻辑（含跨模块终审分发）在 {@link AuditTaskService}。
 *
 * <p>全部接口仅 ADMIN 可用；本 Controller 是认证申请/知识候选终审动作在本系统中<b>唯一</b>的
 * HTTP 入口——{@code AuthApplicationController}/{@code KnowledgeEntryController} 均明确声明
 * 终审路由不在其模块内暴露，统一挂载于此，内部再按 target_type 分发调用对应模块 Service
 * （见 {@code AuditTargetHandler} 实现），不重复实现业务规则。
 */
@RestController
@RequestMapping("/api/v1/audit-tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditTaskController {

    private final AuditTaskService auditTaskService;

    /** FR-M7-01 统一审核队列列表：按 target_type/status 筛选，分页，附各类型待处理数小计。 */
    @GetMapping
    public Result<AuditQueueResponse> queue(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        AuditTaskQuery query = new AuditTaskQuery();
        query.setTargetType(targetType);
        query.setStatus(status);
        query.setKeyword(keyword);
        query.setPage(page);
        query.setSize(size);

        PageResult<AuditTaskBriefDTO> pr = auditTaskService.pageQueue(query);
        Map<String, Long> countByType = auditTaskService.countPendingByType();
        return Result.ok(AuditQueueResponse.builder()
                .records(pr.getRecords())
                .total(pr.getTotal())
                .page(pr.getPage())
                .size(pr.getSize())
                .countByType(countByType)
                .build());
    }

    /** FR-M7-02 审核任务详情：聚合 audit_task + 目标实体详情 + 自动预检提示。 */
    @GetMapping("/{id}")
    public Result<AuditTaskDetailDTO> detail(@PathVariable Long id) {
        return Result.ok(auditTaskService.getById(id));
    }

    /** FR-M7-03/04 单条终审决定：认证申请通过/退回/拒绝、知识候选通过/退回（隐私 checklist 驱动强制转退回）。 */
    @PatchMapping("/{id}/decide")
    public Result<AuditTaskDTO> decide(@PathVariable Long id, @RequestBody DecideRequest request) {
        AuditDecision decision = parseDecision(request.getDecision());
        Long reviewerId = SecurityUtil.currentUserId();
        return Result.ok(auditTaskService.decide(id, reviewerId, decision, request.getChecklistResult(),
                request.getReasonCode(), request.getComment()));
    }

    /** FR-M7-06/07 批量通过/批量退回（仅 KNOWLEDGE_ENTRY 等支持批量的类型，逐条独立事务）。 */
    @PatchMapping("/batch-decide")
    public Result<BatchResultDTO> batchDecide(@RequestBody BatchDecideRequest request) {
        AuditDecision decision = parseDecision(request.getDecision());
        Long reviewerId = SecurityUtil.currentUserId();
        return Result.ok(auditTaskService.batchDecide(request.getTargetType(), request.getIds(), reviewerId,
                decision, request.getReasonCode(), request.getComment()));
    }

    private AuditDecision parseDecision(String raw) {
        try {
            return AuditDecision.valueOf(raw);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "decision 取值不合法（APPROVE/RETURN/REJECT）");
        }
    }
}
