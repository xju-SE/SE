package com.xju.sem.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.module.admin.dto.ApplyContributorCertRequest;
import com.xju.sem.module.admin.dto.AuditTaskDTO;
import com.xju.sem.module.admin.dto.ContributorCertPayload;
import com.xju.sem.module.admin.entity.AuditTask;
import com.xju.sem.module.admin.enums.AdminErrorCode;
import com.xju.sem.module.admin.enums.AuditTargetType;
import com.xju.sem.module.admin.enums.AuditTaskStatus;
import com.xju.sem.module.admin.enums.ReviewKind;
import com.xju.sem.module.admin.mapper.AuditTaskMapper;
import com.xju.sem.module.admin.service.AuditTaskService;
import com.xju.sem.module.admin.service.ContributorCertService;
import com.xju.sem.module.user.constant.Role;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 贡献者认证申请（FR-M7-18）。内部委托 {@link AuditTaskService#createTask} 建任务，申请材料
 * 落 {@code auto_precheck} 列（见 {@link ContributorCertPayload} 类注释的复用理由）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContributorCertServiceImpl implements ContributorCertService {

    private final AuditTaskService auditTaskService;
    private final AuditTaskMapper auditTaskMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AuditTaskDTO apply(Long alumniUserId, ApplyContributorCertRequest request) {
        if (userService.getRole(alumniUserId) != Role.ALUMNI || !userService.isVerified(alumniUserId)) {
            throw new BusinessException(AdminErrorCode.CONTRIBUTOR_CERT_NOT_ELIGIBLE,
                    "仅已认证校友可申请贡献者认证");
        }
        long pending = auditTaskMapper.selectCount(new LambdaQueryWrapper<AuditTask>()
                .eq(AuditTask::getTargetType, AuditTargetType.CONTRIBUTOR_CERT.name())
                .eq(AuditTask::getTargetId, alumniUserId)
                .eq(AuditTask::getStatus, AuditTaskStatus.PENDING.name()));
        if (pending > 0) {
            throw new BusinessException(AdminErrorCode.CONTRIBUTOR_CERT_NOT_ELIGIBLE, "已有待处理的贡献者认证申请");
        }

        AuditTaskDTO task = auditTaskService.createTask(AuditTargetType.CONTRIBUTOR_CERT.name(), alumniUserId,
                alumniUserId, ReviewKind.NEW.name());

        AuditTask patch = new AuditTask();
        patch.setId(task.getId());
        patch.setAutoPrecheck(serialize(new ContributorCertPayload(request.getHonorCertUrl(), request.getNote())));
        auditTaskMapper.updateById(patch);

        return task;
    }

    private String serialize(ContributorCertPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return json.length() > 500 ? json.substring(0, 500) : json;
        } catch (Exception e) {
            log.warn("贡献者认证申请材料序列化失败: {}", e.getMessage());
            return null;
        }
    }
}
