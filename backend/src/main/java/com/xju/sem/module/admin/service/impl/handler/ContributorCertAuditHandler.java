package com.xju.sem.module.admin.service.impl.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xju.sem.module.admin.entity.AuditTask;
import com.xju.sem.module.admin.enums.AuditDecision;
import com.xju.sem.module.admin.enums.AuditTargetType;
import com.xju.sem.module.admin.dto.ContributorCertPayload;
import com.xju.sem.module.admin.mapper.AuditTaskMapper;
import com.xju.sem.module.admin.service.AuditTargetHandler;
import com.xju.sem.module.profile.service.AlumniProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * CONTRIBUTOR_CERT 终审分发（M7 剩余部分补充，07 详细设计 §6.4/FR-M7-19）。跨模块契约：
 * {@code AlumniProfileService.grantContributorBadge(userId, honorCertUrl)}，方法名与 M2 §8 一致。
 * REJECT 无对端调用，仅本表留痕（{@code AuditTaskServiceImpl#notifySubmitter} 已通知申请人）。
 * 不支持批量操作（认证材料需逐条核对荣誉证明，07 详细设计 §6.9 与 AUTH_APPLICATION 同一分工）。
 *
 * <p><b>honorCertUrl 的读取方式</b>：{@link AuditTargetHandler#handle} 接口签名只传
 * {@code targetId}（=申请人 userId），不携带 {@code audit_task.id}；本类直接注入
 * {@link AuditTaskMapper}（同模块内部访问，非跨模块违规）按
 * {@code target_type=CONTRIBUTOR_CERT AND target_id=userId ORDER BY id DESC LIMIT 1} 反查最近一条
 * 任务，解析其 {@code auto_precheck} 列中的 {@link ContributorCertPayload} JSON——
 * {@code ContributorCertServiceImpl.apply} 已保证同一用户同时最多一条 PENDING 申请（重复申请抛
 * 30709），故"按 target_id 取最新一条"在决策时刻能唯一定位到正在被处理的任务，不会新建
 * {@link AuditTargetHandler} 接口签名（避免影响 {@code AuthApplicationAuditHandler}/
 * {@code KnowledgeEntryAuditHandler} 两个既有实现）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContributorCertAuditHandler implements AuditTargetHandler {

    private final AuditTaskMapper auditTaskMapper;
    private final AlumniProfileService alumniProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String targetType() {
        return AuditTargetType.CONTRIBUTOR_CERT.name();
    }

    @Override
    public void handle(Long targetId, Long reviewerId, AuditDecision decision, String comment) {
        if (decision == AuditDecision.APPROVE) {
            String honorCertUrl = readHonorCertUrl(targetId);
            alumniProfileService.grantContributorBadge(targetId, honorCertUrl);
        }
        // REJECT：无对端调用，仅 audit_task 留痕 + 申请人通知（AuditTaskServiceImpl 已处理）
    }

    private String readHonorCertUrl(Long applicantUserId) {
        AuditTask latest = auditTaskMapper.selectOne(new LambdaQueryWrapper<AuditTask>()
                .eq(AuditTask::getTargetType, AuditTargetType.CONTRIBUTOR_CERT.name())
                .eq(AuditTask::getTargetId, applicantUserId)
                .orderByDesc(AuditTask::getId)
                .last("LIMIT 1"));
        if (latest == null || !StringUtils.hasText(latest.getAutoPrecheck())) {
            log.warn("贡献者认证申请材料缺失 userId={}，将以空 honorCertUrl 写入徽章", applicantUserId);
            return null;
        }
        try {
            return objectMapper.readValue(latest.getAutoPrecheck(), ContributorCertPayload.class).getHonorCertUrl();
        } catch (Exception e) {
            log.warn("贡献者认证申请材料解析失败 userId={}: {}", applicantUserId, e.getMessage());
            return null;
        }
    }
}
