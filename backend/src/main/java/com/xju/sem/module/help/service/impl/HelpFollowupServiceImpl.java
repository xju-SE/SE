package com.xju.sem.module.help.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.help.constant.HelpConstants;
import com.xju.sem.module.help.dto.response.HelpFollowupDTO;
import com.xju.sem.module.help.entity.HelpAnswer;
import com.xju.sem.module.help.entity.HelpFollowup;
import com.xju.sem.module.help.entity.HelpTicket;
import com.xju.sem.module.help.enums.HelpTicketStatus;
import com.xju.sem.module.help.mapper.HelpAnswerMapper;
import com.xju.sem.module.help.mapper.HelpFollowupMapper;
import com.xju.sem.module.help.mapper.HelpTicketMapper;
import com.xju.sem.module.help.service.HelpFollowupService;
import com.xju.sem.module.notification.service.NotificationService;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 追问服务实现（§6.3 限次追问校验）。
 *
 * <p>限次口径（对齐 schema 的 help_ticket.followup_count 单计数列 + 任务书"followup_count &lt; limit"）：
 * 仅<strong>求助人</strong>的追问累加计数，达 {@code sem.help.followup-limit} 抛 LIMIT_EXCEEDED；
 * 回答人的回复不计入限次。计数与判定用"仅当未达上限才自增"的原子 UPDATE 完成，避免并发超发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpFollowupServiceImpl implements HelpFollowupService {

    private final HelpFollowupMapper helpFollowupMapper;
    private final HelpTicketMapper helpTicketMapper;
    private final HelpAnswerMapper helpAnswerMapper;
    private final NotificationService notificationService;
    private final UserService userService;

    @Value("${sem.help.followup-limit:3}")
    private int followupLimit;

    @Override
    @Transactional
    public HelpFollowupDTO submitFollowup(Long ticketId, Long senderId, String content) {
        HelpTicket ticket = requireTicket(ticketId);
        if (HelpTicketStatus.CLOSED.name().equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "求助单已关闭，不可追问");
        }
        boolean isAsker = senderId.equals(ticket.getAskerId());
        List<Long> responderIds = distinctResponderIds(ticketId);
        boolean isResponder = responderIds.contains(senderId);
        if (!isAsker && !isResponder) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有求助人或回答人可以在此追问/回复");
        }

        // 求助人追问受限次约束（原子自增，达上限则 rows=0）
        if (isAsker) {
            int rows = helpTicketMapper.incrementFollowupCountIfBelowLimit(ticketId, followupLimit);
            if (rows == 0) {
                throw new BusinessException(ResultCode.LIMIT_EXCEEDED, "该求助追问次数已达上限");
            }
        }

        HelpFollowup followup = new HelpFollowup();
        followup.setTicketId(ticketId);
        followup.setFromUserId(senderId);
        followup.setContent(content);
        helpFollowupMapper.insert(followup);

        // 通知对端：求助人追问→通知回答人们；回答人回复→通知求助人
        if (isAsker) {
            for (Long responderId : responderIds) {
                notifySafe(responderId, HelpConstants.NOTIFY_SYSTEM,
                        "求助人发起了追问", ticket.getTitle(),
                        HelpConstants.REF_HELP_TICKET, ticketId);
            }
        } else {
            notifySafe(ticket.getAskerId(), HelpConstants.NOTIFY_SYSTEM,
                    "回答人回复了你的追问", ticket.getTitle(),
                    HelpConstants.REF_HELP_TICKET, ticketId);
        }

        return toDTO(followup, ticket.getAskerId(), new HashMap<>());
    }

    @Override
    public List<HelpFollowupDTO> listFollowups(Long ticketId, Long viewerId) {
        HelpTicket ticket = requireTicket(ticketId);
        List<HelpFollowup> followups = helpFollowupMapper.selectList(new LambdaQueryWrapper<HelpFollowup>()
                .eq(HelpFollowup::getTicketId, ticketId)
                .orderByAsc(HelpFollowup::getCreatedAt));
        Map<Long, UserBriefDTO> cache = new HashMap<>();
        List<HelpFollowupDTO> result = new ArrayList<>();
        for (HelpFollowup f : followups) {
            result.add(toDTO(f, ticket.getAskerId(), cache));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private List<Long> distinctResponderIds(Long ticketId) {
        List<HelpAnswer> answers = helpAnswerMapper.selectList(new LambdaQueryWrapper<HelpAnswer>()
                .eq(HelpAnswer::getTicketId, ticketId)
                .select(HelpAnswer::getResponderId));
        Set<Long> ids = new LinkedHashSet<>();
        for (HelpAnswer a : answers) {
            if (a.getResponderId() != null) {
                ids.add(a.getResponderId());
            }
        }
        return new ArrayList<>(ids);
    }

    private HelpTicket requireTicket(Long ticketId) {
        HelpTicket ticket = helpTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "求助单不存在");
        }
        return ticket;
    }

    private HelpFollowupDTO toDTO(HelpFollowup f, Long askerId, Map<Long, UserBriefDTO> cache) {
        HelpFollowupDTO dto = new HelpFollowupDTO();
        dto.setId(f.getId());
        dto.setTicketId(f.getTicketId());
        dto.setFromUserId(f.getFromUserId());
        dto.setFromUserName(nameOf(f.getFromUserId(), cache));
        dto.setIsAsker(askerId != null && askerId.equals(f.getFromUserId()));
        dto.setContent(f.getContent());
        dto.setCreatedAt(f.getCreatedAt());
        return dto;
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

    private void notifySafe(Long userId, String type, String title, String content, String refType, Long refId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, type, title, content, refType, refId);
        } catch (Exception e) {
            log.warn("追问通知发送失败 userId={}, ticket={}: {}", userId, refId, e.getMessage());
        }
    }
}
