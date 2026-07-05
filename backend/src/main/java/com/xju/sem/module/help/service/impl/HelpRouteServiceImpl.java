package com.xju.sem.module.help.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.help.constant.HelpConstants;
import com.xju.sem.module.help.dto.response.HelpRouteDTO;
import com.xju.sem.module.help.entity.HelpRoute;
import com.xju.sem.module.help.entity.HelpTicket;
import com.xju.sem.module.help.enums.HelpRouteStatus;
import com.xju.sem.module.help.enums.HelpTicketStatus;
import com.xju.sem.module.help.mapper.CandidateRow;
import com.xju.sem.module.help.mapper.HelpAnswerMapper;
import com.xju.sem.module.help.mapper.HelpRouteMapper;
import com.xju.sem.module.help.mapper.HelpTicketMapper;
import com.xju.sem.module.help.service.HelpRouteService;
import com.xju.sem.module.notification.service.NotificationService;
import com.xju.sem.module.user.constant.Role;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 求助-校友路由匹配算法实现（★系统灵魂 §6.2）。
 *
 * <h3>打分维度（Service 层常量，非硬编码魔法数）</h3>
 * <ul>
 *   <li>W_MAJOR=40：专业相同</li>
 *   <li>W_ALUMNI_IDENTITY=15：候选人为校友身份</li>
 *   <li>W_GRADE_GAP=5/级（封顶3级）：同专业高年级学长学姐的年级差</li>
 *   <li>W_EXPERTISE=6/次（封顶5次）：历史同问题类型被采纳次数（读本模块 help_answer）</li>
 *   <li>W_TRUST=3：历史累计被采纳数的对数信任加权（防头部垄断）</li>
 * </ul>
 *
 * <h3>候选池分层（逐级放宽，直到 ≥ MIN_POOL_SIZE 或穷尽）</h3>
 * <ol>
 *   <li>同专业已认证校友 + 同专业高年级学长（求助人无在读年级即 ALUMNI/ADMIN 发起时，天然只保留校友池）</li>
 *   <li>不足 3 人→全平台已认证校友兜底</li>
 *   <li>仍为空→任一管理员兜底（保证"≥1 次匹配通知"验收标准恒成立）</li>
 * </ol>
 *
 * <p>目标方向（target_direction）在现 schema 为自由文本而非标签外键，本期不纳入打分维度；
 * 未来标签化后可加 W_DIRECTION（GROWTH 命中）。跨表候选查询本期直连 user/student_profile/
 * alumni_profile（任务书授权），未来迁 M2.listVerifiedUsersByMajor，见 {@link HelpRouteMapper}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpRouteServiceImpl implements HelpRouteService {

    private final HelpRouteMapper helpRouteMapper;
    private final HelpTicketMapper helpTicketMapper;
    private final HelpAnswerMapper helpAnswerMapper;
    private final NotificationService notificationService;
    private final UserService userService;

    @Value("${sem.help.route-top-k:5}")
    private int topK;

    private static final int W_MAJOR = 40;
    private static final int W_ALUMNI_IDENTITY = 15;
    private static final int W_GRADE_GAP = 5;
    private static final int W_EXPERTISE = 6;
    private static final int W_TRUST = 3;
    private static final int MAX_GRADE_GAP_COUNTED = 3;
    private static final int MAX_EXPERTISE_COUNTED = 5;
    private static final int MIN_POOL_SIZE = 3;

    private static final String ROLE_ALUMNI = "ALUMNI";
    private static final String ROLE_STUDENT = "STUDENT";

    @Override
    @Transactional
    public void routeHelpTicket(Long ticketId, List<Long> excludeUserIds) {
        HelpTicket ticket = helpTicketMapper.selectById(ticketId);
        if (ticket == null) {
            log.warn("路由匹配跳过：求助单 {} 不存在", ticketId);
            return;
        }
        // 仅对"待匹配/等待回答"的单路由；已回答/采纳/关闭不再打扰候选人
        if (!HelpTicketStatus.OPEN.name().equals(ticket.getStatus())
                && !HelpTicketStatus.MATCHED.name().equals(ticket.getStatus())) {
            log.debug("路由匹配跳过：求助单 {} 状态为 {}", ticketId, ticket.getStatus());
            return;
        }

        // ── 第一步：候选池构建（分层放宽）──
        Set<Long> exclude = new HashSet<>();
        if (excludeUserIds != null) {
            exclude.addAll(excludeUserIds);
        }
        exclude.add(ticket.getAskerId());
        exclude.addAll(helpRouteMapper.listMatchedUserIds(ticketId)); // 已匹配过的不重复通知

        Map<Long, CandidateRow> pool = new HashMap<>();
        // tier1：同专业校友 +（求助人有在读年级时）同专业高年级学长
        if (ticket.getMajorTagId() != null) {
            addCandidates(pool, helpRouteMapper.selectVerifiedAlumniByMajor(ticket.getMajorTagId()), exclude);
            if (ticket.getGradeLevel() != null) {
                addCandidates(pool,
                        helpRouteMapper.selectVerifiedSeniorStudentsByMajor(ticket.getMajorTagId(), ticket.getGradeLevel()),
                        exclude);
            }
        }
        // tier2：不足则全平台校友兜底
        if (pool.size() < MIN_POOL_SIZE) {
            addCandidates(pool, helpRouteMapper.selectAllVerifiedAlumni(), exclude);
        }
        // tier3：仍为空则管理员兜底
        if (pool.isEmpty()) {
            addCandidates(pool, helpRouteMapper.selectAnyAdmin(), exclude);
        }
        if (pool.isEmpty()) {
            log.warn("求助单 {} 无可匹配候选（冷启动无校友/管理员），本轮不产生路由", ticketId);
            return;
        }

        // ── 第二步：逐候选打分 ──
        List<Scored> scoredList = new ArrayList<>();
        for (CandidateRow c : pool.values()) {
            scoredList.add(score(ticket, c));
        }

        // ── 第三步：排序取 TopK（分数降序，userId 升序稳定排序）──
        scoredList.sort((a, b) -> {
            if (b.score != a.score) {
                return Integer.compare(b.score, a.score);
            }
            return Long.compare(a.row.getUserId(), b.row.getUserId());
        });
        int k = Math.max(1, topK);
        List<Scored> winners = scoredList.subList(0, Math.min(k, scoredList.size()));

        // ── 第四步：落库 + 发通知 ──
        int routed = 0;
        for (Scored s : winners) {
            HelpRoute route = new HelpRoute();
            route.setTicketId(ticketId);
            route.setMatchedUserId(s.row.getUserId());
            route.setMatchScore(s.score);
            route.setStatus(HelpRouteStatus.NOTIFIED.name());
            route.setNotifiedAt(LocalDateTime.now());
            try {
                helpRouteMapper.insert(route);
            } catch (Exception e) {
                // uk_ticket_user 兜底：并发/重试竞态下重复命中同一人，跳过即可
                log.debug("路由记录已存在，跳过 ticket={}, user={}", ticketId, s.row.getUserId());
                continue;
            }
            routed++;
            String content = buildNotifyContent(ticket.getTitle(), s.reasons);
            notifySafe(s.row.getUserId(), HelpConstants.NOTIFY_HELP_MATCH,
                    "有一条你可能能解答的求助", content,
                    HelpConstants.REF_HELP_TICKET, ticketId);
        }

        // 命中≥1人：OPEN→MATCHED（重试轮已是 MATCHED 则 CAS 无副作用）
        if (routed > 0) {
            helpTicketMapper.casStatus(ticketId,
                    HelpTicketStatus.OPEN.name(), HelpTicketStatus.MATCHED.name());
        }
        log.info("求助单 {} 路由匹配完成：候选池 {} 人，通知 {} 人", ticketId, pool.size(), routed);
    }

    @Override
    public List<HelpRouteDTO> listRoutes(Long ticketId, Long viewerId) {
        HelpTicket ticket = helpTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "求助单不存在");
        }
        boolean isAsker = viewerId != null && viewerId.equals(ticket.getAskerId());
        boolean isAdmin = viewerId != null && isAdmin(viewerId);
        if (!isAsker && !isAdmin) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅求助人本人或管理员可查看路由记录");
        }
        List<HelpRoute> routes = helpRouteMapper.selectList(new LambdaQueryWrapper<HelpRoute>()
                .eq(HelpRoute::getTicketId, ticketId)
                .orderByDesc(HelpRoute::getMatchScore));
        Map<Long, UserBriefDTO> cache = new HashMap<>();
        List<HelpRouteDTO> result = new ArrayList<>();
        for (HelpRoute r : routes) {
            HelpRouteDTO dto = new HelpRouteDTO();
            dto.setId(r.getId());
            dto.setTicketId(r.getTicketId());
            dto.setMatchedUserId(r.getMatchedUserId());
            dto.setMatchedUserName(nameOf(r.getMatchedUserId(), cache));
            dto.setMatchScore(r.getMatchScore());
            dto.setStatus(r.getStatus());
            dto.setNotifiedAt(r.getNotifiedAt());
            result.add(dto);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // scoring
    // ------------------------------------------------------------------

    private Scored score(HelpTicket ticket, CandidateRow c) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (ticket.getMajorTagId() != null && ticket.getMajorTagId().equals(c.getMajorTagId())) {
            score += W_MAJOR;
            reasons.add("专业相同");
        }
        if (ROLE_ALUMNI.equals(c.getRole())) {
            score += W_ALUMNI_IDENTITY;
            reasons.add("校友身份");
        } else if (ROLE_STUDENT.equals(c.getRole())
                && ticket.getGradeLevel() != null && c.getGradeLevel() != null) {
            int gap = c.getGradeLevel() - ticket.getGradeLevel();
            if (gap > 0) {
                int counted = Math.min(gap, MAX_GRADE_GAP_COUNTED);
                score += counted * W_GRADE_GAP;
                reasons.add("高年级学长学姐");
            }
        }
        // 专长：历史同问题类型被采纳次数（读本模块 help_answer，不依赖 M2 缓存计数）
        if (ticket.getQuestionTypeTagId() != null) {
            int pastSameType = helpAnswerMapper.countAdopted(c.getUserId(), ticket.getQuestionTypeTagId());
            if (pastSameType > 0) {
                score += Math.min(pastSameType, MAX_EXPERTISE_COUNTED) * W_EXPERTISE;
                reasons.add("该类问题曾被采纳" + pastSameType + "次");
            }
        }
        // 信任：累计被采纳数对数缩放
        int totalAdopted = helpAnswerMapper.countAdopted(c.getUserId(), null);
        if (totalAdopted > 0) {
            score += (int) Math.round(W_TRUST * Math.log(1 + totalAdopted));
        }

        Scored s = new Scored();
        s.row = c;
        s.score = score;
        s.reasons = reasons;
        return s;
    }

    private String buildNotifyContent(String title, List<String> reasons) {
        String base = title == null ? "" : title;
        if (reasons == null || reasons.isEmpty()) {
            return base;
        }
        String tail = String.join("/", reasons);
        String content = base + " · " + tail;
        return content.length() > 290 ? content.substring(0, 290) : content;
    }

    private void notifySafe(Long userId, String type, String title, String content, String refType, Long refId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, type, title, content, refType, refId);
        } catch (Exception e) {
            log.warn("路由匹配通知发送失败 userId={}, ticket={}: {}", userId, refId, e.getMessage());
        }
    }

    private void addCandidates(Map<Long, CandidateRow> pool, List<CandidateRow> rows, Set<Long> exclude) {
        if (rows == null) {
            return;
        }
        for (CandidateRow r : rows) {
            if (r.getUserId() == null || exclude.contains(r.getUserId()) || pool.containsKey(r.getUserId())) {
                continue;
            }
            pool.put(r.getUserId(), r);
        }
    }

    private boolean isAdmin(Long userId) {
        try {
            return Role.ADMIN == userService.getRole(userId);
        } catch (Exception e) {
            return false;
        }
    }

    private String nameOf(Long userId, Map<Long, UserBriefDTO> cache) {
        if (userId == null) {
            return null;
        }
        UserBriefDTO brief = cache.computeIfAbsent(userId, id -> {
            try {
                return userService.getBrief(id);
            } catch (Exception e) {
                return null;
            }
        });
        if (brief == null) {
            return null;
        }
        return brief.getRealName() != null && !brief.getRealName().isEmpty()
                ? brief.getRealName() : brief.getUsername();
    }

    /** 打分中间结果。 */
    private static final class Scored {
        private CandidateRow row;
        private int score;
        private List<String> reasons;
    }
}
