package com.xju.sem.module.admin.service.impl.handler;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.admin.enums.AuditDecision;
import com.xju.sem.module.admin.enums.AuditTargetType;
import com.xju.sem.module.admin.service.AuditTargetHandler;
import com.xju.sem.module.user.service.AuthApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AUTH_APPLICATION 终审分发（跨模块契约：AuthApplicationService.approve/reject/returnForSupplement，
 * 方法名与 M1 §8 逐字一致）。批量操作不支持（认证材料需逐条核对，07 详细设计 §6.9）。
 */
@Component
@RequiredArgsConstructor
public class AuthApplicationAuditHandler implements AuditTargetHandler {

    private final AuthApplicationService authApplicationService;

    @Override
    public String targetType() {
        return AuditTargetType.AUTH_APPLICATION.name();
    }

    @Override
    public void handle(Long targetId, Long reviewerId, AuditDecision decision, String comment) {
        switch (decision) {
            case APPROVE -> authApplicationService.approve(targetId, reviewerId);
            case RETURN -> authApplicationService.returnForSupplement(targetId, reviewerId, comment);
            case REJECT -> authApplicationService.reject(targetId, reviewerId, comment);
            default -> throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的终审决定");
        }
    }
}
