package com.xju.sem.module.help.enums;

/**
 * 路由记录状态（对应 help_route.status）。
 * NOTIFIED：已写入路由并已发通知；VIEWED：候选人已查看（本期预留，无写入方）；
 * ANSWERED：该候选人已对本单提交回答（由回答提交事件回写，用于响应率复盘）；
 * EXPIRED：路由过期（本期预留）。
 */
public enum HelpRouteStatus {
    NOTIFIED,
    VIEWED,
    ANSWERED,
    EXPIRED
}
