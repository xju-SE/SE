package com.xju.sem.module.help.service.impl;

import com.xju.sem.module.help.constant.HelpConstants;
import com.xju.sem.module.help.entity.HelpTicket;
import com.xju.sem.module.help.enums.HelpTicketStatus;
import com.xju.sem.module.help.mapper.HelpTicketMapper;
import com.xju.sem.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 求助单自动关闭定时任务（§6.5 任务二，FR-M4-12）。每日凌晨执行两类归档：
 * <ol>
 *   <li>超 {@code TIMEOUT_DAYS} 天且从未采纳的 OPEN/MATCHED/ANSWERED 单 → CLOSED（超时无应答），通知求助人；</li>
 *   <li>采纳后（以 updated_at 近似采纳时刻）超 {@code ADOPTED_GRACE_DAYS} 天的 ADOPTED 单 → CLOSED（宽限期归档）。</li>
 * </ol>
 *
 * <p>关闭用"按读到的原状态 CAS"（{@code casStatus(id, oldStatus, CLOSED)}），并发下若状态已被用户手动
 * 改变则 rows=0 跳过，避免与"求助人手动关闭/采纳"竞态双写。schema 无 close_reason 列，关闭原因仅记日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelpTicketAutoCloseJob {

    private final HelpTicketMapper helpTicketMapper;
    private final NotificationService notificationService;

    private static final int TIMEOUT_DAYS = 7;
    private static final int ADOPTED_GRACE_DAYS = 3;

    @Scheduled(cron = "0 0 2 * * ?")
    public void autoClose() {
        int timeoutClosed = closeTimeout();
        int graceClosed = closeAdoptedGrace();
        if (timeoutClosed > 0 || graceClosed > 0) {
            log.info("求助单自动关闭任务完成：超时关闭 {} 条，采纳宽限关闭 {} 条", timeoutClosed, graceClosed);
        }
    }

    private int closeTimeout() {
        List<HelpTicket> tickets = helpTicketMapper.selectTimeoutClosable(TIMEOUT_DAYS);
        int closed = 0;
        for (HelpTicket t : tickets) {
            try {
                int rows = helpTicketMapper.casStatus(t.getId(), t.getStatus(), HelpTicketStatus.CLOSED.name());
                if (rows > 0) {
                    closed++;
                    log.info("求助单 {} 超时无应答自动关闭（close_reason=TIMEOUT_NO_ANSWER，仅日志留痕）", t.getId());
                    notifySafe(t.getAskerId(), "求助单已自动关闭",
                            "你的求助《" + safeTitle(t) + "》因长期无应答已自动关闭，可重新发布", t.getId());
                }
            } catch (Exception e) {
                log.error("求助单 {} 超时关闭失败: {}", t.getId(), e.getMessage(), e);
            }
        }
        return closed;
    }

    private int closeAdoptedGrace() {
        List<HelpTicket> tickets = helpTicketMapper.selectAdoptedGraceClosable(ADOPTED_GRACE_DAYS);
        int closed = 0;
        for (HelpTicket t : tickets) {
            try {
                int rows = helpTicketMapper.casStatus(t.getId(),
                        HelpTicketStatus.ADOPTED.name(), HelpTicketStatus.CLOSED.name());
                if (rows > 0) {
                    closed++;
                    log.info("求助单 {} 采纳宽限期到期自动归档（close_reason=ADOPTED_DONE，仅日志留痕）", t.getId());
                }
            } catch (Exception e) {
                log.error("求助单 {} 采纳宽限关闭失败: {}", t.getId(), e.getMessage(), e);
            }
        }
        return closed;
    }

    private String safeTitle(HelpTicket t) {
        return t.getTitle() == null ? "" : t.getTitle();
    }

    private void notifySafe(Long userId, String title, String content, Long ticketId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, HelpConstants.NOTIFY_SYSTEM, title, content,
                    HelpConstants.REF_HELP_TICKET, ticketId);
        } catch (Exception e) {
            log.warn("自动关闭通知发送失败 userId={}, ticket={}: {}", userId, ticketId, e.getMessage());
        }
    }
}
