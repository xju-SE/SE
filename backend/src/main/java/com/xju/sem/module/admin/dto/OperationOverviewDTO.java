package com.xju.sem.module.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 运营数据总览（FR-M7-20，07 详细设计 §6.6）。按该文档"优先复用各模块已暴露分页方法的 total
 * 字段，不为纯统计目的要求其他模块新增专用计数接口"的原则实现：全部指标只调用各模块已有的
 * 只读方法拼装，不新增跨模块契约方法（{@code helpResolveRate} 除外——见其字段注释的已知缺口）。
 */
@Data
@Builder
public class OperationOverviewDTO {

    private LocalDate dateFrom;
    private LocalDate dateTo;

    // ---- 认证（M1 AuthApplicationService.pageForReview 的 total 字段） ----
    private long authApprovedCount;
    private long authRejectedCount;
    /** APPROVED / (APPROVED+REJECTED)，两者皆 0 时为 null。 */
    private Double authApprovedRate;

    // ---- 知识库（PUBLISHED 取 M3 KnowledgeEntryService.list 的 total；候选流水线分布取本模块自有 audit_task） ----
    private long knowledgePublishedCount;
    private long knowledgePendingCount;
    private long knowledgeReturnedCount;
    private long knowledgeRejectedCount;

    // ---- 机会（M5 OpportunityService.list，viewerUserId=null 天然排除 PENDING_REVIEW） ----
    private long opportunityPublicCount;

    // ---- 组队（M5 TeamService.list，按四态各查一次求和） ----
    private long teamCount;

    // ---- 举报（本模块自有 report 表） ----
    private long reportPendingCount;
    private long reportHandledCount;
    private long reportDismissedCount;
    /** HANDLED/DISMISSED 且 createdAt 落在统计区间内的处理时长中位数（分钟）；样本为空时 null。
     *  以 updated_at - created_at 近似（schema 无独立 handled_at 列，见 {@code Report} 类注释）。 */
    private Double reportMedianHandleMinutes;

    // ---- 贡献者认证（本模块自有 audit_task，target_type=CONTRIBUTOR_CERT） ----
    private long contributorBadgeCount;

    /**
     * 求助解决率（07 详细设计 §6.6 原始算法项）。<b>本期恒为 null</b>：{@code HelpTicketService}
     * 现有 {@code listTickets(query, viewerId)} 在 {@code majorTagId} 为空时会退化为"取当前登录人
     * 本人专业"（M4 04 实现说明 §4.6），ADMIN 账号无 {@code student_profile}/{@code major_tag_id}，
     * 不适用于全站聚合；07 详细设计 §6.6 又明确"不为统计目的要求其他模块新增专用计数接口"，二者
     * 相叠加使该项在本次不越权、不新增契约的前提下无法产出，留待 M4/M7 协商暴露一个不依赖登录态
     * 专业上下文的全局统计只读方法后再补齐（详见实现说明"假设与简化"）。
     */
    private Double helpResolveRate;
}
