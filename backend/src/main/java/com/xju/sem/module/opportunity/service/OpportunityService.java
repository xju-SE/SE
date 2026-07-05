package com.xju.sem.module.opportunity.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.opportunity.dto.request.CreateOpportunityRequest;
import com.xju.sem.module.opportunity.dto.request.OpportunityQuery;
import com.xju.sem.module.opportunity.dto.request.UpdateOpportunityRequest;
import com.xju.sem.module.opportunity.dto.response.OpportunityBriefDTO;
import com.xju.sem.module.opportunity.dto.response.OpportunityDTO;

/**
 * 机会 Service 接口。跨模块契约方法（{@link #getBrief}/{@link #approve}/{@link #reject}）签名
 * 与地基契约（"M5(提供,供M6引用/M7审核)"）严格一致，供 M6 时间线节点只读引用、M7 终审队列调用；
 * 其余为本模块 Controller 内部使用的方法，签名可自由演进，不受跨模块约束。
 *
 * <p>终审 approve/reject 由 M7 治理端统一审核队列（{@code /api/v1/audit-tasks/{id}/decide}，经
 * {@code AuditTargetHandler} 策略路由）调用本接口，不在 {@code OpportunityController} 暴露独立端点
 * ——与 M1 {@code AuthApplicationService}/M3 {@code KnowledgeEntryService} 终审接口同一模式。
 * 待 M7 侧新增 {@code OpportunityAuditHandler} 并在 {@code AuditTaskEventListener} 补充监听
 * {@link com.xju.sem.module.opportunity.event.OpportunitySubmittedEvent} 后即可接通全链路。
 */
public interface OpportunityService {

    /** FR-M5-01 发布机会；isReferral=true 需发布人为已认证 ALUMNI 且 type=INTERNSHIP，见 §6.1。 */
    OpportunityDTO create(Long publisherId, CreateOpportunityRequest request);

    /**
     * FR-M5-02 编辑机会（发布人/ADMIN，ENDED 不可编辑）。REJECTED 的机会可编辑（S18），
     * 编辑提交后一律回 PENDING_REVIEW 重新终审，解除"被拒不可重提"限制。
     */
    OpportunityDTO update(Long id, Long operatorId, boolean isAdmin, UpdateOpportunityRequest request);

    /** FR-M5-07 详情查看；PENDING_REVIEW 仅发布人/ADMIN 可见，否则按资源不存在处理。 */
    OpportunityDTO getById(Long id, Long viewerUserId, boolean viewerIsAdmin);

    /** 契约方法：供 M6 时间线节点引用 / 首页仪表盘只读引用，签名冻结，不做可见性门控（调用方自控）。 */
    OpportunityBriefDTO getBrief(Long id);

    /** FR-M5-06 列表（类型筛选/即将截止/关键字），仅 viewerUserId 本人可见其 PENDING_REVIEW 记录。 */
    PageResult<OpportunityBriefDTO> list(OpportunityQuery query, Long viewerUserId);

    /** 供 P03 首页仪表盘"即将截止机会"卡片只读聚合展示。 */
    PageResult<OpportunityBriefDTO> listClosingSoon(int limit);

    /** 契约方法：机会终审通过（供 M7 调用）。PENDING_REVIEW → 按 deadline 计算的对外状态。 */
    void approve(Long id, Long reviewerId);

    /** 契约方法：机会终审拒绝（供 M7 调用）。PENDING_REVIEW → REJECTED（S18，发布人可编辑重新提交）。 */
    void reject(Long id, Long reviewerId, String reason);

    /**
     * FR-M5-04/05 结束机会：非 ADMIN 限发布人本人且当前非 ENDED（手动结束）；ADMIN 不受发布人/
     * 当前状态限制（强制下线，用于举报处理）。任一路径均级联结束关联队伍（FR-M5-10，见 §6.7）。
     */
    OpportunityDTO end(Long id, Long operatorId, boolean isAdmin, String reason);

    /**
     * FR-M5-08 简单报名信令。schema 无 apply_count 列（相对 05 详细设计的裁剪，见实现说明"假设与
     * 简化"），本期简化为：校验机会处于 ONGOING/CLOSING_SOON 后通知发布人，不做热度计数持久化。
     */
    void applySignal(Long id, Long userId);

    /** 软删除；发布人仅可删自己发布的机会，ADMIN 任意状态可删。 */
    void delete(Long id, Long operatorId, boolean isAdmin);
}
