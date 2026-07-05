package com.xju.sem.module.help.service.impl;

import com.xju.sem.module.help.event.HelpTicketCreatedEvent;
import com.xju.sem.module.help.service.HelpRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Collections;

/**
 * 求助单创建后触发首次路由匹配（§6.2）。在发布事务提交后异步执行，既不拖慢发布响应，
 * 又保证 {@link HelpRouteService#routeHelpTicket} 读到的是已提交的求助单行。
 * 路由失败仅记日志，不影响发布本身（发布事务此时已提交）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelpTicketCreatedListener {

    private final HelpRouteService helpRouteService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(HelpTicketCreatedEvent event) {
        try {
            helpRouteService.routeHelpTicket(event.getTicketId(), Collections.emptyList());
        } catch (Exception e) {
            log.error("求助单 {} 首次路由匹配失败: {}", event.getTicketId(), e.getMessage(), e);
        }
    }
}
