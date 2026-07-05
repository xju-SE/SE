package com.xju.sem.module.timeline.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.profile.dto.response.StudentProfileDTO;
import com.xju.sem.module.profile.service.StudentProfileService;
import com.xju.sem.module.timeline.dto.response.MyTimelineDTO;
import com.xju.sem.module.timeline.dto.response.OverallProgressDTO;
import com.xju.sem.module.timeline.dto.response.ProgressSummaryDTO;
import com.xju.sem.module.timeline.dto.response.RemediationHintDTO;
import com.xju.sem.module.timeline.dto.response.RouteConfirmResultDTO;
import com.xju.sem.module.timeline.dto.response.StageGroupDTO;
import com.xju.sem.module.timeline.dto.response.StageProgressDTO;
import com.xju.sem.module.timeline.dto.response.TimelineNodeDTO;
import com.xju.sem.module.timeline.dto.response.TimelineNodeItemDTO;
import com.xju.sem.module.timeline.dto.response.TimelineSummaryCardDTO;
import com.xju.sem.module.timeline.dto.response.TimelineTemplateDTO;
import com.xju.sem.module.timeline.dto.response.UserProgressDTO;
import com.xju.sem.module.timeline.entity.TimelineNode;
import com.xju.sem.module.timeline.entity.UserProgress;
import com.xju.sem.module.timeline.enums.ProgressStatus;
import com.xju.sem.module.timeline.enums.RouteType;
import com.xju.sem.module.timeline.enums.Stage;
import com.xju.sem.module.timeline.enums.TimelineErrorCode;
import com.xju.sem.module.timeline.mapper.TimelineNodeMapper;
import com.xju.sem.module.timeline.mapper.UserProgressMapper;
import com.xju.sem.module.timeline.service.TimelineNodeRefService;
import com.xju.sem.module.timeline.service.TimelineTemplateService;
import com.xju.sem.module.timeline.service.UserProgressService;
import com.xju.sem.module.timeline.util.TimelineCalendarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人进度与动态导航服务实现——本模块核心：§6.3 路线解析/切换、§6.4 逾期比对、§6.5 补救打分、
 * §6.6 懒初始化。逾期判定按 <b>stage + suggested_month + enrollYear</b> 换算建议年月后以月粒度比对
 * （对齐任务书"suggested_month vs 当前月"），并给出 daysOverdue 供展示层选择呈现。所有写路径均做
 * 越权防护：目标节点须属该用户当前 {@link #resolveEffective} 解出的有效模板（§9）。
 */
@Service
@RequiredArgsConstructor
public class UserProgressServiceImpl implements UserProgressService {

    private static final int TOP_N = 5;
    private static final int RECENCY_BONUS = 10;

    private final TimelineTemplateService templateService;
    private final TimelineNodeMapper nodeMapper;
    private final UserProgressMapper userProgressMapper;
    private final StudentProfileService studentProfileService;
    private final TimelineNodeRefService nodeRefService;

    // ---------------- FR-M6-05 聚合视图 ----------------

    @Override
    public MyTimelineDTO getMyTimeline(Long userId) {
        Resolved res = resolveEffective(userId);
        if (res.graduated) {
            return MyTimelineDTO.builder()
                    .majorTagId(res.student.getMajorTagId()).graduated(true)
                    .stages(List.of()).overallProgress(zeroProgress()).build();
        }
        if (res.needsDecision) {
            return MyTimelineDTO.builder()
                    .majorTagId(res.student.getMajorTagId())
                    .currentStage(res.currentStage == null ? null : res.currentStage.name())
                    .needsRouteDecision(true).stages(List.of()).overallProgress(zeroProgress()).build();
        }
        int enrollYear = res.student.getEnrollYear();
        List<TimelineNode> nodes = sortedNodes(res.template.getId());
        Map<Long, UserProgress> progressMap = mapByUser(userId);

        // §6.6 懒初始化：UNDECIDED 且本人从未有过该模板任何进度 → 批量 INSERT IGNORE 后重载
        if (RouteType.UNDECIDED.name().equals(res.routeType)
                && !nodes.isEmpty()
                && nodes.stream().noneMatch(n -> progressMap.containsKey(n.getId()))) {
            lazyInit(userId, nodes);
            progressMap = mapByUser(userId);
        }

        List<StageGroupDTO> stages = new ArrayList<>();
        LinkedHashMap<String, List<TimelineNodeItemDTO>> grouped = new LinkedHashMap<>();
        int doneNodes = 0;
        for (TimelineNode n : nodes) {
            String status = statusOf(progressMap, n.getId());
            if (ProgressStatus.DONE.name().equals(status)) {
                doneNodes++;
            }
            OverdueInfo oi = annotate(n, status, enrollYear);
            TimelineNodeItemDTO item = TimelineNodeItemDTO.builder()
                    .node(TimelineMapping.toNodeDTO(n))
                    .progressStatus(status)
                    .suggestedDate(oi.suggestedDate)
                    .overdue(oi.overdue)
                    .monthsOverdue(oi.monthsOverdue)
                    .daysOverdue(oi.daysOverdue)
                    .refs(nodeRefService.listRefs(n.getId()))
                    .build();
            grouped.computeIfAbsent(n.getStage(), k -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<String, List<TimelineNodeItemDTO>> e : grouped.entrySet()) {
            stages.add(StageGroupDTO.builder()
                    .stage(e.getKey()).stageLabel(TimelineMapping.stageLabel(e.getKey()))
                    .nodes(e.getValue()).build());
        }
        return MyTimelineDTO.builder()
                .majorTagId(res.student.getMajorTagId())
                .routeType(res.routeType)
                .currentStage(res.currentStage == null ? null : res.currentStage.name())
                .stages(stages)
                .overallProgress(progress(nodes.size(), doneNodes))
                .build();
    }

    // ---------------- 首页仪表盘摘要卡 ----------------

    @Override
    public TimelineSummaryCardDTO getMySummaryCard(Long userId) {
        Resolved res;
        try {
            res = resolveEffective(userId);
        } catch (BusinessException e) {
            // 模板未配置等 → 首页降级为空卡，不阻断仪表盘
            return TimelineSummaryCardDTO.builder().overallPercentage(0).build();
        }
        if (res.graduated) {
            return TimelineSummaryCardDTO.builder().graduated(true).overallPercentage(0).build();
        }
        if (res.needsDecision) {
            return TimelineSummaryCardDTO.builder().needsRouteDecision(true).overallPercentage(0).build();
        }
        List<TimelineNode> nodes = sortedNodes(res.template.getId());
        Map<Long, UserProgress> progressMap = mapByUser(userId);
        int done = (int) nodes.stream().filter(n -> ProgressStatus.DONE.name().equals(statusOf(progressMap, n.getId()))).count();
        List<RemediationHintDTO> hints = remediation(res, nodes, progressMap);
        return TimelineSummaryCardDTO.builder()
                .routeType(res.routeType)
                .overallPercentage(progress(nodes.size(), done).getPercentage())
                .topRemediationHint(hints.isEmpty() ? null : hints.get(0))
                .build();
    }

    // ---------------- FR-M6-06 路线预览 ----------------

    @Override
    public List<TimelineNodeDTO> previewRoute(Long userId, String routeType) {
        if (!RouteType.isValid(routeType)) {
            throw new BusinessException(TimelineErrorCode.ROUTE_TYPE_INVALID, "路线类型取值不合法");
        }
        StudentProfileDTO student = studentProfileService.getProfile(userId);
        TimelineTemplateDTO tpl = templateService.resolve(student.getMajorTagId(), routeType);
        return sortedNodes(tpl.getId()).stream().map(TimelineMapping::toNodeDTO).toList();
    }

    // ---------------- FR-M6-07 选择/切换路线 ----------------

    @Override
    @Transactional
    public RouteConfirmResultDTO confirmRoute(Long userId, String routeType) {
        if (!RouteType.isValid(routeType)) {
            throw new BusinessException(TimelineErrorCode.ROUTE_TYPE_INVALID, "路线类型取值不合法");
        }
        if (RouteType.UNDECIDED.name().equals(routeType)) {
            throw new BusinessException(TimelineErrorCode.UNDECIDED_NOT_SELECTABLE, "未决策通用线为系统默认态，不可主动选入");
        }
        StudentProfileDTO student = studentProfileService.getProfile(userId);
        TimelineTemplateDTO tpl = templateService.resolve(student.getMajorTagId(), routeType);
        List<TimelineNode> nodes = nodeMapper.listByTemplate(tpl.getId());
        for (TimelineNode n : nodes) {
            userProgressMapper.insertIgnore(userId, n.getId());
        }
        return RouteConfirmResultDTO.builder().routeType(routeType).initializedNodeCount(nodes.size()).build();
    }

    // ---------------- FR-M6-08 标记/切换进度 ----------------

    @Override
    public UserProgressDTO markProgress(Long nodeId, Long userId, String status) {
        if (!ProgressStatus.isValid(status)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "进度状态取值不合法");
        }
        TimelineNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException(TimelineErrorCode.RESOURCE_NOT_FOUND, "时间线节点不存在或已删除");
        }
        Resolved res = resolveEffective(userId);
        if (res.template == null || !node.getTemplateId().equals(res.template.getId())) {
            // 越权防护：只允许标记本人当前有效模板下的节点
            throw new BusinessException(TimelineErrorCode.RESOURCE_NOT_FOUND, "该节点不属于你当前生效的成长时间线");
        }
        LocalDateTime markedAt = ProgressStatus.DONE.name().equals(status) ? LocalDateTime.now() : null;
        UserProgress up = new UserProgress();
        up.setUserId(userId);
        up.setNodeId(nodeId);
        up.setStatus(status);
        up.setMarkedAt(markedAt);
        userProgressMapper.upsert(up);
        return UserProgressDTO.builder().nodeId(nodeId).status(status).markedAt(markedAt).build();
    }

    // ---------------- FR-M6-10 补救优先级 ----------------

    @Override
    public List<RemediationHintDTO> getRemediationHints(Long userId) {
        Resolved res;
        try {
            res = resolveEffective(userId);
        } catch (BusinessException e) {
            return List.of();
        }
        if (res.template == null) {
            return List.of();
        }
        List<TimelineNode> nodes = sortedNodes(res.template.getId());
        return remediation(res, nodes, mapByUser(userId));
    }

    // ---------------- FR-M6-11 完成度统计 ----------------

    @Override
    public ProgressSummaryDTO getProgressSummary(Long userId) {
        Resolved res = resolveEffective(userId);
        if (res.template == null) {
            return ProgressSummaryDTO.builder().overallProgress(zeroProgress()).byStage(List.of()).build();
        }
        List<TimelineNode> nodes = sortedNodes(res.template.getId());
        Map<Long, UserProgress> progressMap = mapByUser(userId);
        LinkedHashMap<String, int[]> byStage = new LinkedHashMap<>();  // stage -> [total, done]
        int totalDone = 0;
        for (TimelineNode n : nodes) {
            boolean done = ProgressStatus.DONE.name().equals(statusOf(progressMap, n.getId()));
            if (done) {
                totalDone++;
            }
            int[] acc = byStage.computeIfAbsent(n.getStage(), k -> new int[2]);
            acc[0]++;
            if (done) {
                acc[1]++;
            }
        }
        List<StageProgressDTO> stageList = new ArrayList<>();
        for (Map.Entry<String, int[]> e : byStage.entrySet()) {
            stageList.add(StageProgressDTO.builder()
                    .stage(e.getKey()).stageLabel(TimelineMapping.stageLabel(e.getKey()))
                    .totalNodes(e.getValue()[0]).doneNodes(e.getValue()[1]).build());
        }
        return ProgressSummaryDTO.builder()
                .overallProgress(progress(nodes.size(), totalDone))
                .byStage(stageList).build();
    }

    // ================= 内部：解析、逾期、补救 =================

    /**
     * §6.3 解析用户当前生效路线与模板。共性阶段（大一~大二上）恒用 UNDECIDED 默认线；决策窗口后
     * 取其最近确认的分化路线，未选则 needsDecision。毕业/超龄 → graduated。缺入学年份 → 30601。
     */
    private Resolved resolveEffective(Long userId) {
        StudentProfileDTO student = studentProfileService.getProfile(userId);
        Integer enrollYear = student.getEnrollYear();
        if (enrollYear == null) {
            throw new BusinessException(TimelineErrorCode.TEMPLATE_NOT_CONFIGURED, "缺少入学年份，无法解析成长时间线");
        }
        Stage currentStage = TimelineCalendarUtil.currentStage(LocalDate.now(), enrollYear);
        if (currentStage == null) {
            return Resolved.graduated(student);
        }
        String routeType;
        if (currentStage.isCommonPhase()) {
            routeType = RouteType.UNDECIDED.name();
        } else {
            routeType = userProgressMapper.findConfirmedRoute(userId);
            if (routeType == null) {
                return Resolved.needsDecision(student, currentStage);
            }
        }
        TimelineTemplateDTO tpl = templateService.resolve(student.getMajorTagId(), routeType);
        return Resolved.of(student, currentStage, routeType, tpl);
    }

    /** §6.5 补救优先级：对已逾期未完成节点打分排序取 TopN。 */
    private List<RemediationHintDTO> remediation(Resolved res, List<TimelineNode> nodes, Map<Long, UserProgress> progressMap) {
        int enrollYear = res.student.getEnrollYear();
        Stage currentStage = res.currentStage;
        Stage prev = currentStage == null ? null : currentStage.previous();
        List<RemediationHintDTO> hints = new ArrayList<>();
        for (TimelineNode n : nodes) {
            String status = statusOf(progressMap, n.getId());
            if (ProgressStatus.DONE.name().equals(status)) {
                continue;
            }
            OverdueInfo oi = annotate(n, status, enrollYear);
            if (!oi.overdue) {
                continue;
            }
            Tier tier = tierOf(oi.monthsOverdue);
            int importance = n.getImportance() == null ? 1 : n.getImportance();
            int recency = currentStage != null
                    && (n.getStage().equals(currentStage.name()) || (prev != null && n.getStage().equals(prev.name())))
                    ? RECENCY_BONUS : 0;
            int score = tier.base * importance + recency;
            hints.add(RemediationHintDTO.builder()
                    .node(TimelineMapping.toNodeDTO(n))
                    .suggestedDate(oi.suggestedDate)
                    .monthsOverdue(oi.monthsOverdue)
                    .daysOverdue(oi.daysOverdue)
                    .priorityTier(tier.name)
                    .priorityScore(score)
                    .build());
        }
        // §6.5 同分 tie-break：daysOverdue 升序（近期逾期优先），再按 node.orderNo 升序（C38 修复：
        // 此前误写成 monthsOverdue 降序=陈年旧账优先，与设计"sortDesc(..., tieBreak=[daysOverdue ASC, orderNo ASC])"方向相反）。
        hints.sort(Comparator.comparingInt(RemediationHintDTO::getPriorityScore).reversed()
                .thenComparingLong(RemediationHintDTO::getDaysOverdue)
                .thenComparingInt(h -> h.getNode().getOrderNo() == null ? 0 : h.getNode().getOrderNo()));
        return hints.stream().limit(TOP_N).toList();
    }

    /** §6.4 单节点逾期标注（月粒度）。DONE 或缺 stage/suggestedMonth → 不逾期。 */
    private OverdueInfo annotate(TimelineNode node, String status, int enrollYear) {
        OverdueInfo info = new OverdueInfo();
        Stage st = Stage.from(node.getStage());
        Integer month = node.getSuggestedMonth();
        if (st == null || month == null) {
            return info;
        }
        YearMonth suggested = TimelineCalendarUtil.suggestedYearMonth(st, month, enrollYear);
        info.suggestedDate = suggested.atEndOfMonth();
        if (ProgressStatus.DONE.name().equals(status)) {
            return info;
        }
        YearMonth current = YearMonth.now();
        if (TimelineCalendarUtil.isOverdue(suggested, current)) {
            info.overdue = true;
            info.monthsOverdue = TimelineCalendarUtil.monthsOverdue(suggested, current);
            info.daysOverdue = TimelineCalendarUtil.daysOverdue(info.suggestedDate, LocalDate.now());
        }
        return info;
    }

    /** §6.5 逾期分档基础分：越紧迫分越高，陈年旧账权重收敛避免霸榜。 */
    private Tier tierOf(int monthsOverdue) {
        if (monthsOverdue <= 1) {
            return new Tier("URGENT", 100);
        }
        if (monthsOverdue <= 3) {
            return new Tier("HIGH", 70);
        }
        if (monthsOverdue <= 6) {
            return new Tier("MEDIUM", 40);
        }
        return new Tier("LOW", 10);
    }

    private void lazyInit(Long userId, List<TimelineNode> nodes) {
        for (TimelineNode n : nodes) {
            userProgressMapper.insertIgnore(userId, n.getId());
        }
    }

    private List<TimelineNode> sortedNodes(Long templateId) {
        return nodeMapper.listByTemplate(templateId).stream().sorted(TimelineMapping.NODE_ORDER).toList();
    }

    private Map<Long, UserProgress> mapByUser(Long userId) {
        Map<Long, UserProgress> map = new LinkedHashMap<>();
        for (UserProgress up : userProgressMapper.listByUser(userId)) {
            map.put(up.getNodeId(), up);
        }
        return map;
    }

    private String statusOf(Map<Long, UserProgress> progressMap, Long nodeId) {
        UserProgress up = progressMap.get(nodeId);
        return up == null ? ProgressStatus.NOT_STARTED.name() : up.getStatus();
    }

    private OverallProgressDTO progress(int total, int done) {
        int pct = total == 0 ? 0 : Math.round(done * 100f / total);
        return OverallProgressDTO.builder().totalNodes(total).doneNodes(done).percentage(pct).build();
    }

    private OverallProgressDTO zeroProgress() {
        return OverallProgressDTO.builder().totalNodes(0).doneNodes(0).percentage(0).build();
    }

    /** 有效路线/模板解析结果（内部传递）。 */
    private static final class Resolved {
        final StudentProfileDTO student;
        final Stage currentStage;
        final String routeType;
        final TimelineTemplateDTO template;
        final boolean needsDecision;
        final boolean graduated;

        private Resolved(StudentProfileDTO student, Stage currentStage, String routeType,
                         TimelineTemplateDTO template, boolean needsDecision, boolean graduated) {
            this.student = student;
            this.currentStage = currentStage;
            this.routeType = routeType;
            this.template = template;
            this.needsDecision = needsDecision;
            this.graduated = graduated;
        }

        static Resolved of(StudentProfileDTO s, Stage cur, String route, TimelineTemplateDTO tpl) {
            return new Resolved(s, cur, route, tpl, false, false);
        }

        static Resolved needsDecision(StudentProfileDTO s, Stage cur) {
            return new Resolved(s, cur, null, null, true, false);
        }

        static Resolved graduated(StudentProfileDTO s) {
            return new Resolved(s, null, null, null, false, true);
        }
    }

    private static final class OverdueInfo {
        boolean overdue;
        int monthsOverdue;
        long daysOverdue;
        LocalDate suggestedDate;
    }

    private static final class Tier {
        final String name;
        final int base;

        Tier(String name, int base) {
            this.name = name;
            this.base = base;
        }
    }
}
