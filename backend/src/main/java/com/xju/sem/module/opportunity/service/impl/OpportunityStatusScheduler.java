package com.xju.sem.module.opportunity.service.impl;

import com.xju.sem.module.opportunity.entity.Opportunity;
import com.xju.sem.module.opportunity.mapper.OpportunityMapper;
import com.xju.sem.module.opportunity.service.TeamService;
import com.xju.sem.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FR-M5-09/10 定时任务：机会状态推进（§6.2）+ ENDED 级联结束关联队伍（§6.7）。
 *
 * <p>与 {@code HelpTicketAutoCloseJob}/{@code KnowledgeEntryExpiryScheduler} 同一纪律：批量
 * CAS UPDATE 各自独立提交，不用一个大事务锁全表；归档扫描按行 CAS 后再触发级联/通知，避免与
 * 用户手动结束产生重复处理竞态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityStatusScheduler {

    private final OpportunityMapper opportunityMapper;
    private final TeamService teamService;
    private final NotificationService notificationService;

    @Value("${sem.opportunity.closing-soon-hours:72}")
    private int closingSoonHours;

    @Value("${sem.opportunity.archive-days:14}")
    private int archiveDays;

    private static final String REF_TYPE = "OPPORTUNITY";

    @Scheduled(fixedRate = 600000) // 每10分钟
    public void advanceStatus() {
        int toClosingSoon = opportunityMapper.advanceOngoingToClosingSoon(closingSoonHours);
        int toClosed = opportunityMapper.advanceToClosed();
        int archived = archiveExpired();
        if (toClosingSoon > 0 || toClosed > 0 || archived > 0) {
            log.info("机会状态推进完成：{}条转CLOSING_SOON，{}条转CLOSED，{}条归档ENDED",
                    toClosingSoon, toClosed, archived);
        }
    }

    private int archiveExpired() {
        List<Opportunity> candidates = opportunityMapper.selectArchivable(archiveDays);
        int archived = 0;
        for (Opportunity o : candidates) {
            // CAS：仅当仍为 CLOSED 时才生效，防止与并发的手动结束/强制下线重复处理
            int rows = opportunityMapper.archiveIfClosed(o.getId());
            if (rows != 1) {
                continue;
            }
            archived++;
            log.info("opportunity {} auto archived to ENDED (deadline={}, archiveDays={})",
                    o.getId(), o.getDeadline(), archiveDays);
            try {
                teamService.endAllByOpportunity(o.getId(), "OPPORTUNITY_ENDED");
            } catch (Exception e) {
                log.error("机会{}归档后级联结束队伍失败: {}", o.getId(), e.getMessage(), e);
            }
            notifySafe(o.getPublisherId(), "机会已归档结束",
                    "你发布的机会《" + o.getTitle() + "》已到归档期限，自动结束", o.getId());
        }
        return archived;
    }

    private void notifySafe(Long userId, String title, String content, Long oppId) {
        if (userId == null) {
            return;
        }
        try {
            notificationService.send(userId, "SYSTEM", title, content, REF_TYPE, oppId);
        } catch (Exception e) {
            log.warn("机会{}归档通知发送失败: {}", oppId, e.getMessage());
        }
    }
}
