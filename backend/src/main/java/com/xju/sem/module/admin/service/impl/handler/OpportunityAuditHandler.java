package com.xju.sem.module.admin.service.impl.handler;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.admin.enums.AuditDecision;
import com.xju.sem.module.admin.enums.AuditTargetType;
import com.xju.sem.module.admin.service.AuditTargetHandler;
import com.xju.sem.module.opportunity.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OPPORTUNITY 终审分发（M7 剩余部分补充，07 详细设计 §6.4/FR-M7-08）。跨模块契约：
 * {@code OpportunityService.approve/reject}，方法名与 M5 §8 一致。无 RETURN 语义——机会内容
 * 相对标准化，不设"退回补充"这一中间态（对齐 M5 状态机：PENDING_REVIEW 直接分叉 通过/拒绝）。
 * 支持批量操作（FR-M7-06/07）。
 */
@Component
@RequiredArgsConstructor
public class OpportunityAuditHandler implements AuditTargetHandler {

    private final OpportunityService opportunityService;

    @Override
    public String targetType() {
        return AuditTargetType.OPPORTUNITY.name();
    }

    @Override
    public boolean supportsBatch() {
        return true;
    }

    @Override
    public void handle(Long targetId, Long reviewerId, AuditDecision decision, String comment) {
        switch (decision) {
            case APPROVE -> opportunityService.approve(targetId, reviewerId);
            case REJECT -> opportunityService.reject(targetId, reviewerId, comment);
            default -> throw new BusinessException(ResultCode.PARAM_INVALID,
                    "机会终审无退回语义，仅支持通过/拒绝");
        }
    }
}
