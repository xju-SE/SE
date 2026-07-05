package com.xju.sem.module.notification.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.notification.dto.response.NotificationDTO;

/**
 * 全局站内通知服务（P17 通知中心的写入与查询统一入口）。
 *
 * <p><b>跨模块契约（全局命名，见地基契约表）</b>：{@link #send} 是本系统内所有"业务事件 → 站内通知"
 * 的唯一落库入口，已被 M1（认证结果/担保确认）、M3（知识条目过期/认领/反馈预警）、M4（路由匹配/
 * 追问/采纳）、M7（审核结果）等多方模块按同一签名调用（详见各模块 {@code service.impl} 包下对本接口
 * 的注入与调用点）。调用方只管"发生了什么事该通知谁"，不关心通知的呈现、已读态、分页——那是本模块
 * 自己的 {@link #pageList}/{@link #markRead}/{@link #markAllRead}/{@link #countUnread} 负责的事，
 * 体现"通知的产生方与消费方解耦"。
 */
public interface NotificationService {

    /**
     * 产生一条站内通知并落库（channel 固定 INAPP，本期不做真实 PUSH 投递）。
     *
     * <p>本方法只做参数校验 + 插入，不抛出"业务不可恢复"类异常之外的任何东西；调用方（M1/M3/M4/M7
     * 等）均已按约定把本调用包在 {@code try/catch} 里且仅记警告日志——通知发送失败不应该、也不会
     * 影响调用方自身主流程（认证审批、采纳、审核终审等）已经完成的动作。
     *
     * @param userId  接收人 user.id，必填
     * @param type    通知类型，须为 HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM 之一（见 {@link com.xju.sem.module.notification.enums.NotificationType}）
     * @param title   标题，必填，超过 100 字自动截断
     * @param content 正文，必填，超过 500 字自动截断
     * @param refType 关联对象类型，可空
     * @param refId   关联对象主键，可空
     */
    void send(Long userId, String type, String title, String content, String refType, Long refId);

    /** P17 通知中心分页列表，可选按 isRead 筛选（null 表示不筛选，全部返回），按创建时间倒序。 */
    PageResult<NotificationDTO> pageList(Long userId, Boolean isRead, int page, int size);

    /** 标记单条已读；不存在抛 NOT_FOUND，非本人通知抛 FORBIDDEN，重复标记幂等成功。 */
    void markRead(Long userId, Long id);

    /** 标记当前用户全部未读通知为已读。 */
    void markAllRead(Long userId);

    /** 当前用户未读通知数（P17 顶部角标）。 */
    long countUnread(Long userId);
}
