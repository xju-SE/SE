package com.xju.sem.module.help.service.impl;

import com.xju.sem.module.help.constant.HelpConstants;
import com.xju.sem.module.help.entity.HelpAnswer;
import com.xju.sem.module.help.entity.HelpTicket;
import com.xju.sem.module.help.event.HelpAnswerAdoptedEvent;
import com.xju.sem.module.help.mapper.HelpAnswerMapper;
import com.xju.sem.module.help.mapper.HelpTicketMapper;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import com.xju.sem.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * 采纳后的跨模块闭环收尾（§6.4 / FR-M4-11）：在采纳事务 <b>提交后</b> 异步执行——
 * <ol>
 *   <li>调 M3 {@link KnowledgeEntryService#createFromHelpAdoption} 生成知识候选（CANDIDATE，自动提交审核）；</li>
 *   <li>回写 help_answer.knowledge_entry_id（链1补列）；</li>
 *   <li>向求助人与回答人发送采纳通知（ADOPT）。</li>
 * </ol>
 *
 * <p>本监听器不加 {@code @Transactional}：M3 的 createFromHelpAdoption 自身为 REQUIRED 事务，在
 * AFTER_COMMIT 无外层事务时会独立开启并提交；回写与通知各自独立执行。任一步失败只记日志走补偿，
 * 绝不回滚已完成的"采纳"动作（下游故障隔离，符合低耦合）。
 *
 * <p>把"采纳→M3"放在独立监听器（而非 HelpAnswerServiceImpl）里，是为了打断
 * {@code KnowledgeEntryService → HelpAnswerService → KnowledgeEntryService} 的构造器循环依赖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelpAnswerAdoptedListener {

    private final KnowledgeEntryService knowledgeEntryService;
    private final HelpAnswerMapper helpAnswerMapper;
    private final HelpTicketMapper helpTicketMapper;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnswerAdopted(HelpAnswerAdoptedEvent event) {
        Long entryId = null;
        try {
            entryId = knowledgeEntryService.createFromHelpAdoption(
                    event.getHelpTicketId(), event.getHelpAnswerId(), event.getAuthorId());
        } catch (Exception e) {
            log.error("采纳后生成知识候选失败 ticket={}, answer={}: {}",
                    event.getHelpTicketId(), event.getHelpAnswerId(), e.getMessage(), e);
        }

        if (entryId != null) {
            try {
                HelpAnswer upd = new HelpAnswer();
                upd.setId(event.getHelpAnswerId());
                upd.setKnowledgeEntryId(entryId);
                helpAnswerMapper.updateById(upd);
            } catch (Exception e) {
                log.error("回写 help_answer.knowledge_entry_id 失败 answer={}, entry={}: {}",
                        event.getHelpAnswerId(), entryId, e.getMessage(), e);
            }
        }

        try {
            HelpTicket ticket = helpTicketMapper.selectById(event.getHelpTicketId());
            String title = ticket == null ? null : ticket.getTitle();
            notifySafe(event.getAuthorId(), "你的回答被采纳", title, event.getHelpAnswerId());
            if (ticket != null) {
                notifySafe(ticket.getAskerId(), "你已采纳最佳回答", title, event.getHelpAnswerId());
            }
        } catch (Exception e) {
            log.warn("采纳通知发送失败 ticket={}: {}", event.getHelpTicketId(), e.getMessage());
        }
    }

    private void notifySafe(Long userId, String title, String content, Long answerId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, HelpConstants.NOTIFY_ADOPT, title, content,
                    HelpConstants.REF_HELP_ANSWER, answerId);
        } catch (Exception e) {
            log.warn("采纳通知发送失败 userId={}: {}", userId, e.getMessage());
        }
    }
}
