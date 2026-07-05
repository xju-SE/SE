package com.xju.sem.module.help.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.help.dto.request.CreateHelpTicketRequest;
import com.xju.sem.module.help.dto.request.HelpTicketQuery;
import com.xju.sem.module.help.dto.response.HelpAnswerDTO;
import com.xju.sem.module.help.dto.response.HelpFollowupDTO;
import com.xju.sem.module.help.dto.response.HelpTicketDTO;
import com.xju.sem.module.help.dto.response.HelpTicketDetailDTO;
import com.xju.sem.module.help.dto.response.HelpTicketListDTO;
import com.xju.sem.module.help.dto.response.MyActionsDTO;
import com.xju.sem.module.help.dto.response.TicketStatCardDTO;
import com.xju.sem.module.help.entity.HelpAnswer;
import com.xju.sem.module.help.entity.HelpFollowup;
import com.xju.sem.module.help.entity.HelpTicket;
import com.xju.sem.module.help.enums.HelpTicketStatus;
import com.xju.sem.module.help.event.HelpTicketCreatedEvent;
import com.xju.sem.module.help.mapper.HelpAnswerMapper;
import com.xju.sem.module.help.mapper.HelpFollowupMapper;
import com.xju.sem.module.help.mapper.HelpTicketMapper;
import com.xju.sem.module.help.service.HelpTicketService;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.dto.UserDTO;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 求助单服务实现（§6.1 快照写入、状态机 CAS 流转、列表统计卡、详情聚合）。
 *
 * <p><b>并发控制</b>：help_ticket 无 version 列，状态流转全部走 {@link HelpTicketMapper} 的
 * "带 WHERE status=? 条件的 CAS UPDATE"，受影响行数为 0 即视为前置状态不满足或并发冲突，转
 * {@link ResultCode#OPTIMISTIC_LOCK}/{@link ResultCode#STATE_CONFLICT}。跨模块只调 {@link UserService}
 * 接口读用户档案/摘要，不直连 M1/M2 的 Mapper 或表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpTicketServiceImpl implements HelpTicketService {

    private final HelpTicketMapper helpTicketMapper;
    private final HelpAnswerMapper helpAnswerMapper;
    private final HelpFollowupMapper helpFollowupMapper;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${sem.help.followup-limit:3}")
    private int followupLimit;

    private static final String VERIFIED = "VERIFIED";

    @Override
    @Transactional
    public HelpTicketDTO createTicket(Long askerId, CreateHelpTicketRequest request) {
        UserDTO asker = userService.getById(askerId);
        if (asker == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!VERIFIED.equals(asker.getAuthStatus())) {
            throw new BusinessException(ResultCode.NOT_VERIFIED, "身份未认证，无法发布求助");
        }
        HelpTicket ticket = new HelpTicket();
        ticket.setAskerId(askerId);
        ticket.setTitle(request.getTitle());
        ticket.setContent(request.getContent());
        // §6.1 专业/年级从发布人档案只读快照写入；ALUMNI/ADMIN 无在读年级，gradeLevel 为 null
        ticket.setMajorTagId(asker.getMajorTagId());
        ticket.setGradeLevel(asker.getGradeLevel());
        ticket.setQuestionTypeTagId(request.getQuestionTypeTagId());
        ticket.setTargetDirection(request.getTargetDirection());
        ticket.setStatus(HelpTicketStatus.OPEN.name());
        ticket.setFollowupCount(0);
        helpTicketMapper.insert(ticket);

        // 解耦触发 §6.2 路由匹配：AFTER_COMMIT 异步执行，既不拖慢发布响应，又保证读到已提交行
        eventPublisher.publishEvent(new HelpTicketCreatedEvent(ticket.getId()));
        return toTicketDTO(ticket, displayName(asker), 0);
    }

    @Override
    public HelpTicketDetailDTO getDetail(Long ticketId, Long viewerId) {
        HelpTicket ticket = requireTicket(ticketId);
        List<HelpAnswer> answers = helpAnswerMapper.selectList(new LambdaQueryWrapper<HelpAnswer>()
                .eq(HelpAnswer::getTicketId, ticketId)
                .orderByAsc(HelpAnswer::getCreatedAt));
        List<HelpFollowup> followups = helpFollowupMapper.selectList(new LambdaQueryWrapper<HelpFollowup>()
                .eq(HelpFollowup::getTicketId, ticketId)
                .orderByAsc(HelpFollowup::getCreatedAt));

        Map<Long, UserBriefDTO> briefCache = new HashMap<>();

        HelpTicketDetailDTO detail = new HelpTicketDetailDTO();
        detail.setTicket(toTicketDTO(ticket, nameOf(ticket.getAskerId(), briefCache), answers.size()));

        List<HelpAnswerDTO> answerDTOs = new ArrayList<>();
        for (HelpAnswer a : answers) {
            answerDTOs.add(toAnswerDTO(a, briefCache));
        }
        detail.setAnswers(answerDTOs);

        List<HelpFollowupDTO> followupDTOs = new ArrayList<>();
        for (HelpFollowup f : followups) {
            followupDTOs.add(toFollowupDTO(f, ticket.getAskerId(), briefCache));
        }
        detail.setFollowups(followupDTOs);

        detail.setMyActions(buildActions(ticket, viewerId, answers));
        return detail;
    }

    @Override
    public HelpTicketListDTO listTickets(HelpTicketQuery query, Long viewerId) {
        int page = (query.getPage() == null || query.getPage() <= 0) ? 1 : query.getPage();
        int size = (query.getSize() == null || query.getSize() <= 0) ? 10 : Math.min(query.getSize(), 50);

        Long majorTagId = query.getMajorTagId();
        if (majorTagId == null) {
            UserDTO viewer = viewerId == null ? null : userService.getById(viewerId);
            majorTagId = viewer == null ? null : viewer.getMajorTagId();
        }

        LambdaQueryWrapper<HelpTicket> qw = new LambdaQueryWrapper<>();
        if (majorTagId != null) {
            qw.eq(HelpTicket::getMajorTagId, majorTagId);
        }
        if (query.getQuestionTypeTagId() != null) {
            qw.eq(HelpTicket::getQuestionTypeTagId, query.getQuestionTypeTagId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            if (!HelpTicketStatus.isValid(query.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "状态取值不合法");
            }
            qw.eq(HelpTicket::getStatus, query.getStatus());
        }
        applySort(qw, query.getSortBy());

        Page<HelpTicket> result = helpTicketMapper.selectPage(new Page<>(page, size), qw);

        Map<Long, UserBriefDTO> briefCache = new HashMap<>();
        List<HelpTicketDTO> records = new ArrayList<>();
        for (HelpTicket t : result.getRecords()) {
            int answerCount = helpAnswerMapper.countByTicket(t.getId());
            records.add(toTicketDTO(t, nameOf(t.getAskerId(), briefCache), answerCount));
        }

        HelpTicketListDTO dto = new HelpTicketListDTO();
        dto.setRecords(records);
        dto.setTotal(result.getTotal());
        dto.setPage(result.getCurrent());
        dto.setSize(result.getSize());
        // 统计卡仅在按具体专业过滤时计算（跨专业浏览无"本专业"语义）
        if (majorTagId != null) {
            TicketStatCardDTO card = new TicketStatCardDTO();
            card.setOpenCount(helpTicketMapper.countOpenByMajor(majorTagId));
            card.setResolvedCount(helpTicketMapper.countResolvedByMajor(majorTagId));
            card.setAvgResponseHours(helpTicketMapper.avgResponseHoursByMajor(majorTagId));
            dto.setStatCard(card);
        }
        return dto;
    }

    @Override
    @Transactional
    public void withdraw(Long ticketId, Long operatorId) {
        HelpTicket ticket = requireTicket(ticketId);
        if (!operatorId.equals(ticket.getAskerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能撤回自己发布的求助");
        }
        boolean withdrawable = HelpTicketStatus.OPEN.name().equals(ticket.getStatus())
                || HelpTicketStatus.MATCHED.name().equals(ticket.getStatus());
        if (!withdrawable) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "当前状态不可撤回");
        }
        if (helpAnswerMapper.countByTicket(ticketId) > 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "已有回答的求助不可撤回，请改用关闭");
        }
        helpTicketMapper.deleteById(ticketId);
    }

    @Override
    @Transactional
    public void close(Long ticketId, Long operatorId, String closeReason) {
        HelpTicket ticket = requireTicket(ticketId);
        if (!operatorId.equals(ticket.getAskerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能关闭自己发布的求助");
        }
        if (HelpTicketStatus.CLOSED.name().equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "求助单已关闭");
        }
        int rows = helpTicketMapper.closeTicket(ticketId);
        if (rows == 0) {
            // 并发下被定时任务/他处先行关闭
            throw new BusinessException(ResultCode.OPTIMISTIC_LOCK);
        }
        // schema 无 close_reason 列，本期仅日志留痕（见实现说明"假设与简化"）
        log.info("help_ticket {} closed by user {}, reason={}", ticketId, operatorId, closeReason);
    }

    @Override
    public HelpTicketDTO getSummary(Long ticketId) {
        HelpTicket ticket = requireTicket(ticketId);
        int answerCount = helpAnswerMapper.countByTicket(ticketId);
        return toTicketDTO(ticket, nameOf(ticket.getAskerId(), new HashMap<>()), answerCount);
    }

    @Override
    @Transactional
    public void hideTicket(Long ticketId, Long adminOperatorId, String reason) {
        requireTicket(ticketId);
        // schema 无独立 HIDDEN 状态列，复用 deleted 逻辑删除位软处置：隐藏后自动从
        // 列表/详情/统计卡查询中排除，语义等价"下架"（对齐 M2 hidePathCardByReport 既有模式）。
        int rows = helpTicketMapper.deleteById(ticketId);
        if (rows == 0) {
            throw new BusinessException(ResultCode.OPTIMISTIC_LOCK);
        }
        log.warn("help_ticket {} hidden by admin {}, reason={}", ticketId, adminOperatorId, reason);
    }

    @Override
    @Transactional
    public void restoreTicket(Long ticketId, Long adminOperatorId) {
        int rows = helpTicketMapper.restoreTicket(ticketId);
        if (rows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "求助单不存在或未处于隐藏状态");
        }
        log.warn("help_ticket {} restored by admin {}", ticketId, adminOperatorId);
    }

    @Override
    public TicketStatCardDTO getMyCollegeOpenTicketStats(Long userId) {
        UserDTO user = userService.getById(userId);
        TicketStatCardDTO card = new TicketStatCardDTO();
        Long majorTagId = user == null ? null : user.getMajorTagId();
        if (majorTagId == null) {
            card.setOpenCount(0L);
            card.setResolvedCount(0L);
            return card;
        }
        // help_ticket 无 college 列，以本人 majorTagId 作为学院粒度代理范围（与 listTickets
        // 统计卡口径一致，见接口 javadoc 说明）。
        card.setOpenCount(helpTicketMapper.countOpenByMajor(majorTagId));
        card.setResolvedCount(helpTicketMapper.countResolvedByMajor(majorTagId));
        card.setAvgResponseHours(helpTicketMapper.avgResponseHoursByMajor(majorTagId));
        return card;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void applySort(LambdaQueryWrapper<HelpTicket> qw, String sortBy) {
        if ("NEARLY_TIMEOUT".equals(sortBy)) {
            qw.orderByAsc(HelpTicket::getCreatedAt);
        } else if ("UNANSWERED_FIRST".equals(sortBy)) {
            // 无人回应优先：先 OPEN/MATCHED，再 ANSWERED，最后 ADOPTED/CLOSED；同组内新单在前
            qw.last("ORDER BY FIELD(status,'OPEN','MATCHED','ANSWERED','ADOPTED','CLOSED'), created_at DESC");
        } else {
            qw.orderByDesc(HelpTicket::getCreatedAt);
        }
    }

    private MyActionsDTO buildActions(HelpTicket ticket, Long viewerId, List<HelpAnswer> answers) {
        MyActionsDTO actions = new MyActionsDTO();
        String status = ticket.getStatus();
        boolean isAsker = viewerId != null && viewerId.equals(ticket.getAskerId());
        boolean answered = false;
        if (viewerId != null) {
            for (HelpAnswer a : answers) {
                if (viewerId.equals(a.getResponderId())) {
                    answered = true;
                    break;
                }
            }
        }
        boolean answerable = HelpTicketStatus.OPEN.name().equals(status)
                || HelpTicketStatus.MATCHED.name().equals(status)
                || HelpTicketStatus.ANSWERED.name().equals(status);
        actions.setCanAnswer(viewerId != null && !isAsker && !answered && answerable);
        actions.setCanAdopt(isAsker && HelpTicketStatus.ANSWERED.name().equals(status));

        boolean closed = HelpTicketStatus.CLOSED.name().equals(status);
        int used = ticket.getFollowupCount() == null ? 0 : ticket.getFollowupCount();
        if (isAsker) {
            // 求助人追问受限次约束
            actions.setCanFollowUp(!closed && used < followupLimit);
        } else if (answered) {
            // 回答人回复不计限次
            actions.setCanFollowUp(!closed);
        } else {
            actions.setCanFollowUp(false);
        }
        return actions;
    }

    private HelpTicket requireTicket(Long ticketId) {
        HelpTicket ticket = helpTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "求助单不存在");
        }
        return ticket;
    }

    private HelpTicketDTO toTicketDTO(HelpTicket t, String askerName, int answerCount) {
        HelpTicketDTO dto = new HelpTicketDTO();
        dto.setId(t.getId());
        dto.setAskerId(t.getAskerId());
        dto.setAskerName(askerName);
        dto.setTitle(t.getTitle());
        dto.setContent(t.getContent());
        dto.setMajorTagId(t.getMajorTagId());
        dto.setGradeLevel(t.getGradeLevel());
        dto.setQuestionTypeTagId(t.getQuestionTypeTagId());
        dto.setTargetDirection(t.getTargetDirection());
        dto.setStatus(t.getStatus());
        dto.setFollowupCount(t.getFollowupCount());
        dto.setAnswerCount(answerCount);
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }

    private HelpAnswerDTO toAnswerDTO(HelpAnswer a, Map<Long, UserBriefDTO> briefCache) {
        HelpAnswerDTO dto = new HelpAnswerDTO();
        dto.setId(a.getId());
        dto.setTicketId(a.getTicketId());
        dto.setResponderId(a.getResponderId());
        UserBriefDTO brief = briefOf(a.getResponderId(), briefCache);
        dto.setResponderName(brief == null ? null : displayName(brief));
        dto.setResponderRole(brief == null ? null : brief.getRole());
        dto.setPrecondition(a.getPrecondition());
        dto.setSteps(a.getSteps());
        dto.setCautions(a.getCautions());
        dto.setIsAdopted(a.getIsAdopted());
        dto.setKnowledgeEntryId(a.getKnowledgeEntryId());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    private HelpFollowupDTO toFollowupDTO(HelpFollowup f, Long askerId, Map<Long, UserBriefDTO> briefCache) {
        HelpFollowupDTO dto = new HelpFollowupDTO();
        dto.setId(f.getId());
        dto.setTicketId(f.getTicketId());
        dto.setFromUserId(f.getFromUserId());
        dto.setFromUserName(nameOf(f.getFromUserId(), briefCache));
        dto.setIsAsker(askerId != null && askerId.equals(f.getFromUserId()));
        dto.setContent(f.getContent());
        dto.setCreatedAt(f.getCreatedAt());
        return dto;
    }

    private String nameOf(Long userId, Map<Long, UserBriefDTO> briefCache) {
        UserBriefDTO brief = briefOf(userId, briefCache);
        return brief == null ? null : displayName(brief);
    }

    private UserBriefDTO briefOf(Long userId, Map<Long, UserBriefDTO> briefCache) {
        if (userId == null) {
            return null;
        }
        if (briefCache.containsKey(userId)) {
            return briefCache.get(userId);
        }
        UserBriefDTO brief = null;
        try {
            brief = userService.getBrief(userId);
        } catch (Exception e) {
            log.debug("获取用户摘要失败 userId={}: {}", userId, e.getMessage());
        }
        briefCache.put(userId, brief);
        return brief;
    }

    private String displayName(UserBriefDTO brief) {
        if (brief == null) {
            return null;
        }
        return StringUtils.hasText(brief.getRealName()) ? brief.getRealName() : brief.getUsername();
    }

    private String displayName(UserDTO user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }
}
