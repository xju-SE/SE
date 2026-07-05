package com.xju.sem.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.admin.dto.HandleReportRequest;
import com.xju.sem.module.admin.dto.ReportDTO;
import com.xju.sem.module.admin.dto.ReportQuery;
import com.xju.sem.module.admin.dto.SubmitReportRequest;
import com.xju.sem.module.admin.entity.Report;
import com.xju.sem.module.admin.enums.AdminErrorCode;
import com.xju.sem.module.admin.enums.ReportDecision;
import com.xju.sem.module.admin.enums.ReportHandleAction;
import com.xju.sem.module.admin.enums.ReportReasonType;
import com.xju.sem.module.admin.enums.ReportStatus;
import com.xju.sem.module.admin.enums.ReportTargetType;
import com.xju.sem.module.admin.mapper.ReportMapper;
import com.xju.sem.module.admin.service.ReportService;
import com.xju.sem.module.knowledge.dto.request.OfflineRequest;
import com.xju.sem.module.knowledge.dto.response.KnowledgeBriefDTO;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import com.xju.sem.module.help.service.HelpTicketService;
import com.xju.sem.module.notification.service.NotificationService;
import com.xju.sem.module.opportunity.dto.response.OpportunityBriefDTO;
import com.xju.sem.module.opportunity.service.OpportunityService;
import com.xju.sem.module.profile.dto.response.VisiblePathCardDTO;
import com.xju.sem.module.profile.service.AlumniPathCardService;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 举报受理实现（FR-M7-09~12，07 详细设计 §6.4 dispatchReportAction/§9）。
 *
 * <p><b>处置分发的已知缺口（HELP_TICKET）</b>：{@code HelpTicketService} 目前只暴露
 * {@code close(ticketId, operatorId, reason)}，且其内部严格校验
 * {@code operatorId.equals(ticket.getAskerId())}（求助人本人专属，无 ADMIN 放行分支，见 M4 04
 * 实现说明 §4.7 第 7 条"hideTicket/restoreTicket 本期未实现,留待相应模块落地时按需补齐"）——
 * ADMIN 调用会必然抛 FORBIDDEN。本类不越权直连 M4 Mapper/entity，也不在 M4 未开放能力前假装
 * 调用会成功：对 {@code HELP_TICKET} 的 UPHELD 举报只允许 {@code handleAction=NONE}
 * （记录成立、不自动处置，需人工线下跟进），选择其余动作码抛
 * {@link AdminErrorCode#REPORT_ACTION_UNSUPPORTED}，待 M4 补充管理员强制关闭方法后再接通。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    /** 折叠文本格式："[CODE] 说明"，CODE 部分做只读解析展示，不影响落库。 */
    private static final Pattern CODE_PREFIX = Pattern.compile("^\\[([A-Z_]+)]\\s*(.*)$", Pattern.DOTALL);

    private final ReportMapper reportMapper;
    private final UserService userService;
    private final HelpTicketService helpTicketService;
    private final KnowledgeEntryService knowledgeEntryService;
    private final AlumniPathCardService alumniPathCardService;
    private final OpportunityService opportunityService;
    private final NotificationService notificationService;

    @Override
    public ReportDTO submit(Long reporterId, SubmitReportRequest request) {
        if (!ReportTargetType.isValid(request.getTargetType())) {
            throw new BusinessException(AdminErrorCode.REPORT_PARAM_INVALID, "targetType 取值不合法");
        }
        if (!ReportReasonType.isValid(request.getReasonType())) {
            throw new BusinessException(AdminErrorCode.REPORT_PARAM_INVALID, "reasonType 取值不合法");
        }
        validateTargetExists(request.getTargetType(), request.getTargetId());

        String reasonText = fold(request.getReasonType(), request.getDescription());

        // FR-M7-09 去重：同一 reporter 对同一 target 已有 PENDING 记录时合并说明而非新建
        Report existing = reportMapper.selectOne(new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, request.getTargetType())
                .eq(Report::getTargetId, request.getTargetId())
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getStatus, ReportStatus.PENDING.name())
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setReason(reasonText);
            reportMapper.updateById(existing);
            return toDTO(existing);
        }

        Report report = new Report();
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReporterId(reporterId);
        report.setReason(reasonText);
        report.setStatus(ReportStatus.PENDING.name());
        reportMapper.insert(report);
        return toDTO(report);
    }

    @Override
    public PageResult<ReportDTO> pageForAdmin(ReportQuery query) {
        if (StringUtils.hasText(query.getTargetType()) && !ReportTargetType.isValid(query.getTargetType())) {
            throw new BusinessException(AdminErrorCode.REPORT_PARAM_INVALID, "targetType 取值不合法");
        }
        String status = StringUtils.hasText(query.getStatus()) ? query.getStatus() : ReportStatus.PENDING.name();

        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<>();
        qw.eq(Report::getStatus, status);
        if (StringUtils.hasText(query.getTargetType())) {
            qw.eq(Report::getTargetType, query.getTargetType());
        }
        qw.orderByAsc(Report::getCreatedAt);
        IPage<Report> page = reportMapper.selectPage(pageOf(query.getPage(), query.getSize()), qw);
        List<ReportDTO> records = page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<ReportDTO> pageMine(Long reporterId, String status, int page, int size) {
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<>();
        qw.eq(Report::getReporterId, reporterId);
        if (StringUtils.hasText(status)) {
            qw.eq(Report::getStatus, status);
        }
        qw.orderByDesc(Report::getCreatedAt);
        IPage<Report> p = reportMapper.selectPage(pageOf(page, size), qw);
        List<ReportDTO> records = p.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public ReportDTO getById(Long id) {
        return toDTO(requireExisting(id));
    }

    @Override
    @Transactional
    public ReportDTO handle(Long id, Long adminId, HandleReportRequest request) {
        Report report = requireExisting(id);
        if (!ReportStatus.PENDING.name().equals(report.getStatus())) {
            throw new BusinessException(AdminErrorCode.REPORT_STATE_CONFLICT, "该举报已被处理");
        }
        ReportDecision decision = parseDecision(request.getDecision());

        ReportHandleAction action = ReportHandleAction.NONE;
        if (decision == ReportDecision.UPHELD) {
            if (!StringUtils.hasText(request.getHandleAction())) {
                throw new BusinessException(AdminErrorCode.REPORT_PARAM_INVALID, "举报成立时必须选择处理动作");
            }
            action = ReportHandleAction.ofCodeOrNone(request.getHandleAction());
            requireActionMatch(report.getTargetType(), action);
        }

        String note = fold(action.name(), request.getHandleComment());
        int rows = reportMapper.casHandle(id, decision.toStatus(), adminId, note);
        if (rows == 0) {
            throw new BusinessException(AdminErrorCode.REPORT_STATE_CONFLICT, "该举报已被处理，请刷新后重试");
        }

        if (decision == ReportDecision.UPHELD && action != ReportHandleAction.NONE) {
            // 与 audit_task.decide() 同一强一致策略：report 状态 CAS 更新与目标模块治理动作复用同一
            // 物理事务（@Transactional，propagation=REQUIRED），任一环节失败整体回滚，不出现
            // "举报已标记成立但内容其实未下线/未封禁"的中间态（07 详细设计 §6.4 设计要点同理适用）
            dispatch(report.getTargetType(), report.getTargetId(), adminId, request.getHandleComment());
        }

        notifyReporterSafe(report.getReporterId(), decision, report.getTargetType(), report.getTargetId());

        Report updated = reportMapper.selectById(id);
        return toDTO(updated);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void requireActionMatch(String targetType, ReportHandleAction action) {
        if (action == ReportHandleAction.NONE) {
            return;
        }
        ReportHandleAction expected;
        if (ReportTargetType.KNOWLEDGE_ENTRY.name().equals(targetType)
                || ReportTargetType.OPPORTUNITY.name().equals(targetType)) {
            expected = ReportHandleAction.CONTENT_OFFLINE;
        } else if (ReportTargetType.ALUMNI_PATH_CARD.name().equals(targetType)) {
            expected = ReportHandleAction.CONTENT_HIDDEN;
        } else if (ReportTargetType.USER.name().equals(targetType)) {
            expected = ReportHandleAction.USER_DISABLED;
        } else {
            // HELP_TICKET：本期无任何非 NONE 动作可用，见类注释"已知缺口"
            throw new BusinessException(AdminErrorCode.REPORT_ACTION_UNSUPPORTED,
                    "该类型举报处置本期暂不支持自动执行（M4 尚未开放管理员强制关闭入口），请选择 NONE 后人工线下处理");
        }
        if (action != expected) {
            throw new BusinessException(AdminErrorCode.REPORT_ACTION_MISMATCH,
                    "handleAction 与 targetType 不匹配，该类型仅支持 " + expected.name());
        }
    }

    private void dispatch(String targetType, Long targetId, Long adminId, String comment) {
        if (ReportTargetType.KNOWLEDGE_ENTRY.name().equals(targetType)) {
            OfflineRequest req = new OfflineRequest();
            req.setReason(comment);
            knowledgeEntryService.offline(targetId, adminId, true, req);
        } else if (ReportTargetType.ALUMNI_PATH_CARD.name().equals(targetType)) {
            alumniPathCardService.hidePathCardByReport(targetId, adminId, comment);
        } else if (ReportTargetType.OPPORTUNITY.name().equals(targetType)) {
            opportunityService.end(targetId, adminId, true, comment);
        } else if (ReportTargetType.USER.name().equals(targetType)) {
            userService.disableUser(targetId, comment);
        }
        // HELP_TICKET 不会走到这里：requireActionMatch 已拦截其一切非 NONE 动作
    }

    private void validateTargetExists(String targetType, Long targetId) {
        try {
            if (ReportTargetType.HELP_TICKET.name().equals(targetType)) {
                helpTicketService.getDetail(targetId, null);
            } else if (ReportTargetType.KNOWLEDGE_ENTRY.name().equals(targetType)) {
                if (!knowledgeEntryService.existsPublished(targetId)) {
                    throw new BusinessException(ResultCode.NOT_FOUND, "举报目标不存在");
                }
            } else if (ReportTargetType.ALUMNI_PATH_CARD.name().equals(targetType)) {
                if (!alumniPathCardService.existsPathCard(targetId)) {
                    throw new BusinessException(ResultCode.NOT_FOUND, "举报目标不存在");
                }
            } else if (ReportTargetType.OPPORTUNITY.name().equals(targetType)) {
                opportunityService.getBrief(targetId);
            } else if (ReportTargetType.USER.name().equals(targetType)) {
                userService.getBrief(targetId);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报目标不存在");
        }
    }

    private String resolveTargetSummary(String targetType, Long targetId) {
        try {
            if (ReportTargetType.KNOWLEDGE_ENTRY.name().equals(targetType)) {
                KnowledgeBriefDTO brief = knowledgeEntryService.getBrief(targetId);
                return brief.getTitle();
            }
            if (ReportTargetType.OPPORTUNITY.name().equals(targetType)) {
                OpportunityBriefDTO brief = opportunityService.getBrief(targetId);
                return brief.getTitle();
            }
            if (ReportTargetType.ALUMNI_PATH_CARD.name().equals(targetType)) {
                VisiblePathCardDTO card = alumniPathCardService.getVisiblePathCard(targetId, null);
                return card == null ? null : ("校友路径卡#" + targetId);
            }
            if (ReportTargetType.USER.name().equals(targetType)) {
                UserBriefDTO brief = userService.getBrief(targetId);
                return StringUtils.hasText(brief.getRealName()) ? brief.getRealName() : brief.getUsername();
            }
            if (ReportTargetType.HELP_TICKET.name().equals(targetType)) {
                return "求助单#" + targetId;
            }
        } catch (Exception e) {
            log.warn("解析举报目标摘要失败 targetType={} targetId={}: {}", targetType, targetId, e.getMessage());
        }
        return null;
    }

    private void notifyReporterSafe(Long reporterId, ReportDecision decision, String targetType, Long targetId) {
        if (reporterId == null) {
            return;
        }
        try {
            String title = decision == ReportDecision.UPHELD ? "举报已处理：成立" : "举报已处理：不成立";
            notificationService.send(reporterId, "AUDIT_RESULT", title,
                    "你提交的举报（" + targetType + "）已处理完成", targetType, targetId);
        } catch (Exception e) {
            log.warn("举报处理结果通知发送失败: {}", e.getMessage());
        }
    }

    private String fold(String code, String text) {
        String body = StringUtils.hasText(text) ? text.trim() : "";
        String combined = "[" + code + "] " + body;
        return combined.length() > 300 ? combined.substring(0, 300) : combined;
    }

    private ReportDecision parseDecision(String raw) {
        try {
            return ReportDecision.valueOf(raw);
        } catch (Exception e) {
            throw new BusinessException(AdminErrorCode.REPORT_PARAM_INVALID, "decision 取值不合法（UPHELD/DISMISSED）");
        }
    }

    private Report requireExisting(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        return report;
    }

    private Page<Report> pageOf(int page, int size) {
        int p = page <= 0 ? 1 : page;
        int s = size <= 0 ? 10 : Math.min(size, 50);
        return new Page<>(p, s);
    }

    private ReportDTO toDTO(Report r) {
        String[] reasonParts = splitCode(r.getReason());
        String[] handleParts = splitCode(r.getHandleNote());
        String reporterName = null;
        try {
            UserBriefDTO brief = userService.getBrief(r.getReporterId());
            reporterName = StringUtils.hasText(brief.getRealName()) ? brief.getRealName() : brief.getUsername();
        } catch (Exception e) {
            log.warn("解析举报人展示名失败 reporterId={}: {}", r.getReporterId(), e.getMessage());
        }
        boolean isPending = ReportStatus.PENDING.name().equals(r.getStatus());
        return ReportDTO.builder()
                .id(r.getId())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .targetSummary(resolveTargetSummary(r.getTargetType(), r.getTargetId()))
                .reporterId(r.getReporterId())
                .reporterName(reporterName)
                .reasonType(reasonParts[0])
                .description(reasonParts[1])
                .status(r.getStatus())
                .handlerId(r.getHandlerId())
                .handleAction(handleParts[0])
                .handleComment(handleParts[1])
                .createdAt(r.getCreatedAt())
                .handledAt(isPending ? null : r.getUpdatedAt())
                .build();
    }

    /** 拆解 "[CODE] 文本" 折叠格式；无法解析时整体归入文本部分，CODE 置 null。 */
    private String[] splitCode(String folded) {
        if (!StringUtils.hasText(folded)) {
            return new String[]{null, null};
        }
        Matcher m = CODE_PREFIX.matcher(folded);
        if (m.matches()) {
            return new String[]{m.group(1), m.group(2)};
        }
        return new String[]{null, folded};
    }
}
