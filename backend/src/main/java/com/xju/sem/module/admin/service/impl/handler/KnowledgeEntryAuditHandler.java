package com.xju.sem.module.admin.service.impl.handler;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.admin.enums.AuditDecision;
import com.xju.sem.module.admin.enums.AuditTargetType;
import com.xju.sem.module.admin.service.AuditTargetHandler;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * KNOWLEDGE_ENTRY 终审分发（跨模块契约：KnowledgeEntryService.approve/returnToCandidate，
 * 方法名与 M3 §8 一致）。无独立 REJECT 语义——与 M3 状态机一致（退回可反复修订，不做一次性报废）。
 * 支持批量操作（FR-M7-06/07）。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeEntryAuditHandler implements AuditTargetHandler {

    private final KnowledgeEntryService knowledgeEntryService;

    @Override
    public String targetType() {
        return AuditTargetType.KNOWLEDGE_ENTRY.name();
    }

    @Override
    public boolean supportsBatch() {
        return true;
    }

    @Override
    public void handle(Long targetId, Long reviewerId, AuditDecision decision, String comment) {
        switch (decision) {
            case APPROVE -> knowledgeEntryService.approve(targetId, reviewerId);
            case RETURN -> knowledgeEntryService.returnToCandidate(targetId, reviewerId, comment);
            case REJECT -> throw new BusinessException(ResultCode.PARAM_INVALID,
                    "知识候选无独立拒绝语义，仅支持通过/退回");
            default -> throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的终审决定");
        }
    }
}
