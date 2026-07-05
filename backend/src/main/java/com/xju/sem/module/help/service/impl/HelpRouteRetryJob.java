package com.xju.sem.module.help.service.impl;

import com.xju.sem.module.help.entity.HelpTicket;
import com.xju.sem.module.help.mapper.HelpRouteMapper;
import com.xju.sem.module.help.mapper.HelpTicketMapper;
import com.xju.sem.module.help.service.HelpRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由重试与兜底升级定时任务（§6.5 任务一，FR-M4-03）。每 30 分钟扫描仍 OPEN/MATCHED 且零应答的
 * 求助单，排除已通知过的候选后再跑一轮 {@link HelpRouteService#routeHelpTicket}（其候选池分层放宽逻辑
 * 会自动纳入更大范围的校友），保证长期无人应答的单持续获得新一批匹配通知，且不重复打扰同一人。
 *
 * <p>逐单独立 try/catch，单条失败不影响整批；每次只对"新候选"发通知，候选耗尽后本任务对该单自然静默。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelpRouteRetryJob {

    private final HelpTicketMapper helpTicketMapper;
    private final HelpRouteMapper helpRouteMapper;
    private final HelpRouteService helpRouteService;

    /** 创建满 30 分钟仍零应答才纳入重试，避免与创建时的即时路由争抢。 */
    private static final int MIN_AGE_MINUTES = 30;

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void retryRouting() {
        List<HelpTicket> tickets = helpTicketMapper.selectRetryable(MIN_AGE_MINUTES);
        if (tickets.isEmpty()) {
            return;
        }
        log.info("路由重试任务启动，待重试求助单 {} 条", tickets.size());
        for (HelpTicket ticket : tickets) {
            try {
                List<Long> alreadyNotified = helpRouteMapper.listMatchedUserIds(ticket.getId());
                helpRouteService.routeHelpTicket(ticket.getId(), alreadyNotified);
            } catch (Exception e) {
                log.error("求助单 {} 路由重试失败: {}", ticket.getId(), e.getMessage(), e);
            }
        }
    }
}
