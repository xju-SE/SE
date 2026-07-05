package com.xju.sem.module.admin.service;

import com.xju.sem.module.admin.enums.AuditDecision;

/**
 * 审核终审目标分发策略（策略路由，07 详细设计 §6.4/§9）。{@code AuditTaskServiceImpl} 持有
 * {@code Map<targetType, AuditTargetHandler>}，新增 target_type 时只需新增实现类并注册为
 * Spring Bean，不改动 decide()/batchDecide() 主流程，符合开闭原则。
 */
public interface AuditTargetHandler {

    /** 本 Handler 负责的 target_type（对应 {@link com.xju.sem.module.admin.enums.AuditTargetType} 枚举名）。 */
    String targetType();

    /** 是否支持批量操作（FR-M7-06/07 仅结构化、无强隐私红线的类型开放批量）。 */
    default boolean supportsBatch() {
        return false;
    }

    /** 执行终审动作；不支持的 decision（如 KNOWLEDGE_ENTRY 无 REJECT 语义）应抛 BusinessException。 */
    void handle(Long targetId, Long reviewerId, AuditDecision decision, String comment);
}
