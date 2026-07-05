package com.xju.sem.module.help.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.help.constant.HelpConstants;
import com.xju.sem.module.help.dto.AnswerContentDTO;
import com.xju.sem.module.help.dto.request.SubmitAnswerRequest;
import com.xju.sem.module.help.dto.response.HelpAnswerDTO;
import com.xju.sem.module.help.entity.HelpAnswer;
import com.xju.sem.module.help.entity.HelpTicket;
import com.xju.sem.module.help.enums.HelpTicketStatus;
import com.xju.sem.module.help.event.HelpAnswerAdoptedEvent;
import com.xju.sem.module.help.mapper.HelpAnswerMapper;
import com.xju.sem.module.help.mapper.HelpRouteMapper;
import com.xju.sem.module.help.mapper.HelpTicketMapper;
import com.xju.sem.module.help.service.HelpAnswerService;
import com.xju.sem.module.notification.service.NotificationService;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 回答服务实现（§6.4 采纳、模板化回答校验、跨模块闭环事件触发）。
 *
 * <p><b>事务边界</b>：{@link #adopt} 的 {@code @Transactional} 只覆盖"置 is_adopted + CAS
 * ANSWERED→ADOPTED + 回写路由响应"这一最小核心写；生成知识候选（M3）、回写 knowledge_entry_id、
 * 采纳通知统一交给 {@code HelpAnswerAdoptedListener} 在 AFTER_COMMIT 异步完成，任一下游失败仅记日志、
 * 不回滚已成功的采纳动作（低耦合，§9）。
 *
 * <p>本类不依赖 {@code KnowledgeEntryService}——采纳→M3 的调用放在独立监听器里，从而避免
 * "KnowledgeEntryService → HelpAnswerService → KnowledgeEntryService" 的构造器循环依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpAnswerServiceImpl implements HelpAnswerService {

    private final HelpAnswerMapper helpAnswerMapper;
    private final HelpTicketMapper helpTicketMapper;
    private final HelpRouteMapper helpRouteMapper;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    /** steps JSON 落库列 VARCHAR(2000)，正文合计上限留足序列化余量。 */
    private static final int MAX_STEPS_TOTAL_CHARS = 1500;
    private static final int MAX_STEPS_COUNT = 20;

    @Override
    @Transactional
    public HelpAnswerDTO submitAnswer(Long ticketId, Long responderId, SubmitAnswerRequest request) {
        HelpTicket ticket = requireTicket(ticketId);
        if (responderId.equals(ticket.getAskerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能回答自己发布的求助");
        }
        if (!answerableStatus(ticket.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "当前状态不可提交回答");
        }
        // 同一人对同一求助单只保留一条回答（schema 未落 uk，应用级去重）
        Long existing = helpAnswerMapper.selectCount(new LambdaQueryWrapper<HelpAnswer>()
                .eq(HelpAnswer::getTicketId, ticketId)
                .eq(HelpAnswer::getResponderId, responderId));
        if (existing != null && existing > 0) {
            throw new BusinessException(ResultCode.DUPLICATE, "您已回答过该求助，请编辑我的回答");
        }
        List<String> steps = normalizeSteps(request.getSteps());

        HelpAnswer answer = new HelpAnswer();
        answer.setTicketId(ticketId);
        answer.setResponderId(responderId);
        answer.setPrecondition(request.getPrecondition());
        answer.setSteps(steps);
        answer.setCautions(request.getCautions());
        answer.setIsAdopted(0);
        helpAnswerMapper.insert(answer);

        // 首条回答：OPEN/MATCHED → ANSWERED（幂等 CAS，已 ANSWERED 不变）
        helpTicketMapper.markAnswered(ticketId);
        // 若回答人本身是路由命中者，回写其已响应（响应率复盘）
        helpRouteMapper.markResponded(ticketId, responderId);
        // 通知求助人
        notifySafe(ticket.getAskerId(), HelpConstants.NOTIFY_SYSTEM,
                "你的求助收到新回答", ticket.getTitle(),
                HelpConstants.REF_HELP_ANSWER, answer.getId());

        return toAnswerDTO(answer);
    }

    @Override
    @Transactional
    public HelpAnswerDTO editAnswer(Long answerId, Long operatorId, SubmitAnswerRequest request) {
        HelpAnswer answer = requireAnswer(answerId);
        if (!operatorId.equals(answer.getResponderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑自己的回答");
        }
        if (answer.getIsAdopted() != null && answer.getIsAdopted() == 1) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "已被采纳的回答不可再编辑");
        }
        List<String> steps = normalizeSteps(request.getSteps());
        answer.setPrecondition(request.getPrecondition());
        answer.setSteps(steps);
        answer.setCautions(request.getCautions());
        helpAnswerMapper.updateById(answer);
        return toAnswerDTO(answer);
    }

    @Override
    @Transactional
    public void adopt(Long ticketId, Long answerId, Long operatorId) {
        HelpTicket ticket = requireTicket(ticketId);
        if (!operatorId.equals(ticket.getAskerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有求助人本人可以采纳");
        }
        if (!HelpTicketStatus.ANSWERED.name().equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅在已有回答且未采纳时可采纳");
        }
        HelpAnswer answer = requireAnswer(answerId);
        if (!ticketId.equals(answer.getTicketId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该回答不属于此求助单");
        }
        if (answer.getIsAdopted() != null && answer.getIsAdopted() == 1) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该回答已被采纳");
        }

        // 最小核心写：置采纳位 + CAS 求助单状态
        HelpAnswer upd = new HelpAnswer();
        upd.setId(answerId);
        upd.setIsAdopted(1);
        helpAnswerMapper.updateById(upd);

        int rows = helpTicketMapper.casStatus(ticketId,
                HelpTicketStatus.ANSWERED.name(), HelpTicketStatus.ADOPTED.name());
        if (rows == 0) {
            // 并发下状态已变（如被关闭/已采纳），乐观锁冲突
            throw new BusinessException(ResultCode.OPTIMISTIC_LOCK);
        }
        // 采纳者若为路由命中人，回写响应
        helpRouteMapper.markResponded(ticketId, answer.getResponderId());

        // 事务提交后异步：M3 生成候选 + 回写 knowledge_entry_id + 采纳通知（见 HelpAnswerAdoptedListener）
        eventPublisher.publishEvent(new HelpAnswerAdoptedEvent(ticketId, answerId, answer.getResponderId()));
    }

    @Override
    public AnswerContentDTO getForCandidate(Long answerId) {
        HelpAnswer answer = helpAnswerMapper.selectById(answerId);
        if (answer == null) {
            return null;
        }
        AnswerContentDTO dto = new AnswerContentDTO();
        HelpTicket ticket = helpTicketMapper.selectById(answer.getTicketId());
        dto.setTicketTitle(ticket == null ? null : ticket.getTitle());
        dto.setPrecondition(answer.getPrecondition());
        dto.setSteps(renderSteps(answer.getSteps()));
        dto.setCautions(answer.getCautions());
        return dto;
    }

    @Override
    public int countAdopted(Long responderId, Long questionTypeTagId) {
        return helpAnswerMapper.countAdopted(responderId, questionTypeTagId);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private boolean answerableStatus(String status) {
        return HelpTicketStatus.OPEN.name().equals(status)
                || HelpTicketStatus.MATCHED.name().equals(status)
                || HelpTicketStatus.ANSWERED.name().equals(status);
    }

    /** 清洗步骤：去空白项、校验条数与总长；空则报参数错误。 */
    private List<String> normalizeSteps(List<String> raw) {
        List<String> cleaned = new ArrayList<>();
        if (raw != null) {
            for (String s : raw) {
                if (StringUtils.hasText(s)) {
                    cleaned.add(s.trim());
                }
            }
        }
        if (cleaned.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "操作步骤不能为空");
        }
        if (cleaned.size() > MAX_STEPS_COUNT) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "步骤条数不能超过" + MAX_STEPS_COUNT);
        }
        int total = 0;
        for (String s : cleaned) {
            total += s.length();
        }
        if (total > MAX_STEPS_TOTAL_CHARS) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "步骤内容合计不能超过" + MAX_STEPS_TOTAL_CHARS + "字");
        }
        return cleaned;
    }

    /** 步骤数组渲染为带序号纯文本，供 M3 拼装知识候选正文。 */
    private String renderSteps(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i + 1).append(". ").append(steps.get(i));
        }
        return sb.toString();
    }

    private HelpTicket requireTicket(Long ticketId) {
        HelpTicket ticket = helpTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "求助单不存在");
        }
        return ticket;
    }

    private HelpAnswer requireAnswer(Long answerId) {
        HelpAnswer answer = helpAnswerMapper.selectById(answerId);
        if (answer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "回答不存在");
        }
        return answer;
    }

    private HelpAnswerDTO toAnswerDTO(HelpAnswer a) {
        HelpAnswerDTO dto = new HelpAnswerDTO();
        dto.setId(a.getId());
        dto.setTicketId(a.getTicketId());
        dto.setResponderId(a.getResponderId());
        UserBriefDTO brief = safeBrief(a.getResponderId());
        if (brief != null) {
            dto.setResponderName(StringUtils.hasText(brief.getRealName())
                    ? brief.getRealName() : brief.getUsername());
            dto.setResponderRole(brief.getRole());
        }
        dto.setPrecondition(a.getPrecondition());
        dto.setSteps(a.getSteps());
        dto.setCautions(a.getCautions());
        dto.setIsAdopted(a.getIsAdopted());
        dto.setKnowledgeEntryId(a.getKnowledgeEntryId());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    private UserBriefDTO safeBrief(Long userId) {
        try {
            return userService.getBrief(userId);
        } catch (Exception e) {
            log.debug("获取回答人摘要失败 userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    private void notifySafe(Long userId, String type, String title, String content, String refType, Long refId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, type, title, content, refType, refId);
        } catch (Exception e) {
            log.warn("回答相关通知发送失败 userId={}, type={}: {}", userId, type, e.getMessage());
        }
    }
}
