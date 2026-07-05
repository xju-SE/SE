package com.xju.sem.module.help.event;

import lombok.Getter;

/**
 * 求助单创建事件。由 {@code HelpTicketServiceImpl.createTicket} 在其 {@code @Transactional} 内发布，
 * Spring 于事务提交后（{@code @TransactionalEventListener(AFTER_COMMIT)}）投递给
 * {@code HelpTicketCreatedListener}，触发 §6.2 路由匹配算法——把匹配从发布请求主链路上解耦，
 * 既不拖慢发布响应时延，又保证路由读到的是已提交的求助单行。与 M1 AuthApplicationSubmittedEvent
 * 同一解耦风格。
 */
@Getter
public class HelpTicketCreatedEvent {

    private final Long ticketId;

    public HelpTicketCreatedEvent(Long ticketId) {
        this.ticketId = ticketId;
    }
}
