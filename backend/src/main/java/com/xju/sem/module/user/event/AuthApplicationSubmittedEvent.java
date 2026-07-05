package com.xju.sem.module.user.event;

import lombok.Getter;

/**
 * 认证申请进入待审计/待审核状态时发布（跨模块契约事件）。
 * <p>M1 在 {@code @Transactional} 方法内发布，Spring 于事务提交后投递；
 * M7 用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 监听并创建 audit_task：
 * autoApproved=true（SSO/邀请码自动通过）直接落 AUTO_APPROVED 留痕，false 进入人工审核队列。
 * <p>M7 仅凭 appId 回读 auth_application 获取其余字段，不复制列。
 */
@Getter
public class AuthApplicationSubmittedEvent {

    private final Long appId;
    private final boolean autoApproved;

    public AuthApplicationSubmittedEvent(Long appId, boolean autoApproved) {
        this.appId = appId;
        this.autoApproved = autoApproved;
    }
}
