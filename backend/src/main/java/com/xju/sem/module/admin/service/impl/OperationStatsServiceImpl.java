package com.xju.sem.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.module.admin.dto.AuditThroughputStatsDTO;
import com.xju.sem.module.admin.dto.DailyDecidedCountDTO;
import com.xju.sem.module.admin.dto.OperationOverviewDTO;
import com.xju.sem.module.admin.entity.AuditTask;
import com.xju.sem.module.admin.entity.Report;
import com.xju.sem.module.admin.enums.AuditTargetType;
import com.xju.sem.module.admin.enums.AuditTaskStatus;
import com.xju.sem.module.admin.enums.ReportStatus;
import com.xju.sem.module.admin.mapper.AuditTaskMapper;
import com.xju.sem.module.admin.mapper.DailyCount;
import com.xju.sem.module.admin.mapper.ReportMapper;
import com.xju.sem.module.admin.mapper.StatusCount;
import com.xju.sem.module.admin.service.OperationStatsService;
import com.xju.sem.module.opportunity.dto.request.OpportunityQuery;
import com.xju.sem.module.opportunity.dto.request.TeamQuery;
import com.xju.sem.module.opportunity.enums.TeamStatus;
import com.xju.sem.module.opportunity.service.OpportunityService;
import com.xju.sem.module.opportunity.service.TeamService;
import com.xju.sem.module.user.dto.AuthApplicationQuery;
import com.xju.sem.module.user.service.AuthApplicationService;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运营数据统计看板实现（FR-M7-20/21，07 详细设计 §6.5/§6.6）。原则："优先复用各模块已暴露分页
 * 方法的 total 字段，不为纯统计目的要求其他模块新增专用计数接口"——除本模块自有的
 * {@code audit_task}/{@code report} 两张表可直接聚合外，其余指标一律通过已存在的跨模块只读方法
 * （{@code pageForReview}/{@code list}）取 {@code total} 字段拼装，不新增任何跨模块契约方法。
 */
@Service
@RequiredArgsConstructor
public class OperationStatsServiceImpl implements OperationStatsService {

    private final AuditTaskMapper auditTaskMapper;
    private final ReportMapper reportMapper;
    private final AuthApplicationService authApplicationService;
    private final KnowledgeEntryService knowledgeEntryService;
    private final OpportunityService opportunityService;
    private final TeamService teamService;

    // ---- §6.5 PDAT 基准值（可配置常量，"便于答辩时调参展示"） ----
    @Value("${sem.admin.pdat.admin-online-count:2}")
    private int adminOnlineCount;
    @Value("${sem.admin.pdat.daily-available-minutes:45}")
    private int dailyAvailableMinutes;
    @Value("${sem.admin.pdat.avg-seconds.auth-application:90}")
    private int avgSecondsAuth;
    @Value("${sem.admin.pdat.avg-seconds.knowledge-entry:150}")
    private int avgSecondsKnowledge;
    @Value("${sem.admin.pdat.avg-seconds.opportunity:60}")
    private int avgSecondsOpportunity;
    @Value("${sem.admin.pdat.avg-seconds.contributor-cert:60}")
    private int avgSecondsContributorCert;

    @Override
    public OperationOverviewDTO getOverview(LocalDate from, LocalDate to) {
        LocalDate[] range = normalizeRange(from, to);
        LocalDate dateFrom = range[0];
        LocalDate dateTo = range[1];

        // ---- 认证：M1 AuthApplicationService.pageForReview 的 total 字段 ----
        long authApproved = authTotal("APPROVED");
        long authRejected = authTotal("REJECTED");
        Double authRate = (authApproved + authRejected) == 0 ? null
                : (double) authApproved / (authApproved + authRejected);

        // ---- 知识库：PUBLISHED 取 M3 list 的 total；候选流水线分布取本模块自有 audit_task ----
        long knowledgePublished = knowledgeEntryService.list(null, null, 1, 1).getTotal();
        Map<String, Long> knowledgeAuditByStatus = auditTaskMapper
                .countByTargetTypeGroupStatus(AuditTargetType.KNOWLEDGE_ENTRY.name()).stream()
                .collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getCnt));

        // ---- 机会：M5 list，viewerUserId=null 天然排除 PENDING_REVIEW（等价"对外公开"口径） ----
        OpportunityQuery oq = new OpportunityQuery();
        oq.setIncludeEnded(true);
        long opportunityPublic = opportunityService.list(oq, null).getTotal();

        // ---- 组队：M5 list 按四态各查一次求和（TeamQuery 契约"status 为空默认仅 RECRUITING"，
        //      故不能传空一次性取全部，只能逐态求和，不新增跨模块方法） ----
        long teamCount = 0;
        for (TeamStatus s : TeamStatus.values()) {
            TeamQuery tq = new TeamQuery();
            tq.setStatus(s.name());
            teamCount += teamService.list(tq).getTotal();
        }

        // ---- 举报：本模块自有表 ----
        long reportPending = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, ReportStatus.PENDING.name()));
        long reportHandled = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, ReportStatus.HANDLED.name()));
        long reportDismissed = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, ReportStatus.DISMISSED.name()));
        Double medianHandleMinutes = computeMedianHandleMinutes(dateFrom, dateTo);

        // ---- 贡献者认证：本模块自有 audit_task ----
        long contributorBadgeCount = auditTaskMapper.selectCount(new LambdaQueryWrapper<AuditTask>()
                .eq(AuditTask::getTargetType, AuditTargetType.CONTRIBUTOR_CERT.name())
                .eq(AuditTask::getStatus, AuditTaskStatus.APPROVED.name()));

        return OperationOverviewDTO.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .authApprovedCount(authApproved)
                .authRejectedCount(authRejected)
                .authApprovedRate(authRate)
                .knowledgePublishedCount(knowledgePublished)
                .knowledgePendingCount(knowledgeAuditByStatus.getOrDefault(AuditTaskStatus.PENDING.name(), 0L))
                .knowledgeReturnedCount(knowledgeAuditByStatus.getOrDefault(AuditTaskStatus.RETURNED.name(), 0L))
                .knowledgeRejectedCount(knowledgeAuditByStatus.getOrDefault(AuditTaskStatus.REJECTED.name(), 0L))
                .opportunityPublicCount(opportunityPublic)
                .teamCount(teamCount)
                .reportPendingCount(reportPending)
                .reportHandledCount(reportHandled)
                .reportDismissedCount(reportDismissed)
                .reportMedianHandleMinutes(medianHandleMinutes)
                .contributorBadgeCount(contributorBadgeCount)
                .helpResolveRate(null) // 已知缺口，见 OperationOverviewDTO#helpResolveRate 字段注释
                .build();
    }

    @Override
    public AuditThroughputStatsDTO getAuditThroughput(LocalDate from, LocalDate to) {
        LocalDate[] range = normalizeRange(from, to);
        List<DailyCount> raw = auditTaskMapper.dailyDecidedCounts(range[0], range[1]);
        List<DailyDecidedCountDTO> daily = raw.stream()
                .map(d -> new DailyDecidedCountDTO(d.getDay(), d.getCnt()))
                .collect(Collectors.toList());
        return AuditThroughputStatsDTO.builder()
                .dateFrom(range[0])
                .dateTo(range[1])
                .dailyDecided(daily)
                .peakDailyThroughputEstimate(estimatePeakDailyThroughput())
                .build();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** §6.5 PDAT = Σ_type(admin_online_count × daily_available_minutes × 60 ÷ avg_decision_seconds[type])。 */
    private double estimatePeakDailyThroughput() {
        double budgetSeconds = (double) adminOnlineCount * dailyAvailableMinutes * 60;
        return budgetSeconds / avgSecondsAuth
                + budgetSeconds / avgSecondsKnowledge
                + budgetSeconds / avgSecondsOpportunity
                + budgetSeconds / avgSecondsContributorCert;
    }

    private long authTotal(String status) {
        AuthApplicationQuery q = new AuthApplicationQuery();
        q.setStatus(status);
        q.setPage(1);
        q.setSize(1);
        return authApplicationService.pageForReview(q).getTotal();
    }

    private Double computeMedianHandleMinutes(LocalDate from, LocalDate to) {
        List<Report> rows = reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .in(Report::getStatus, List.of(ReportStatus.HANDLED.name(), ReportStatus.DISMISSED.name()))
                .ge(Report::getCreatedAt, from.atStartOfDay())
                .le(Report::getCreatedAt, LocalDateTime.of(to, LocalTime.MAX)));
        List<Long> minutes = new ArrayList<>();
        for (Report r : rows) {
            if (r.getCreatedAt() != null && r.getUpdatedAt() != null) {
                minutes.add(Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toMinutes());
            }
        }
        if (minutes.isEmpty()) {
            return null;
        }
        Collections.sort(minutes);
        int n = minutes.size();
        return n % 2 == 1 ? minutes.get(n / 2).doubleValue()
                : (minutes.get(n / 2 - 1) + minutes.get(n / 2)) / 2.0;
    }

    private LocalDate[] normalizeRange(LocalDate from, LocalDate to) {
        LocalDate dateTo = to != null ? to : LocalDate.now();
        LocalDate dateFrom = from != null ? from : dateTo.minusDays(6);
        return new LocalDate[]{dateFrom, dateTo};
    }
}
