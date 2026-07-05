package com.xju.sem.module.user.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.user.dto.AuthApplicationDTO;
import com.xju.sem.module.user.dto.AuthApplicationQuery;
import com.xju.sem.module.user.dto.BatchInviteCodeRequest;
import com.xju.sem.module.user.dto.InviteCodeCheckDTO;
import com.xju.sem.module.user.dto.ResubmitAuthApplicationRequest;
import com.xju.sem.module.user.dto.SubmitAuthApplicationRequest;

import java.util.List;

/**
 * 认证申请服务：三条分级认证路径 + 邀请码机制 + 终审状态机。
 * 审核类更新一律状态 CAS 防并发；提交后发布 AuthApplicationSubmittedEvent 解耦 M7 建 audit_task。
 */
public interface AuthApplicationService {

    /** 提交认证申请（按 verifyMethod 分支：SSO 自动核验 / 人工 / 邀请码认领 / 双担保）。 */
    AuthApplicationDTO submit(Long userId, SubmitAuthApplicationRequest request);

    /** 申请详情（本人/ADMIN）。 */
    AuthApplicationDTO getById(Long id);

    /** 我的认证申请历史（分页）。 */
    PageResult<AuthApplicationDTO> pageMine(Long userId, long page, long size);

    /** 供 M7 审核列表调用的分页查询。 */
    PageResult<AuthApplicationDTO> pageForReview(AuthApplicationQuery query);

    /** 撤回（PENDING/AWAITING_GUARANTEE 前）。 */
    AuthApplicationDTO withdraw(Long id, Long userId);

    /** RETURNED 后补充重新提交。 */
    AuthApplicationDTO resubmit(Long id, Long userId, ResubmitAuthApplicationRequest request);

    /** 担保人确认/拒绝。 */
    AuthApplicationDTO confirmGuarantee(Long id, Long guarantorUserId, boolean approve);

    // ---- 跨模块契约：M7 治理端 Controller 调用，做状态机流转与档案回写 ----

    /** 终审通过：UNDER_REVIEW→APPROVED，回写 user.auth_status=VERIFIED 与对应 profile。 */
    void approve(Long appId, Long reviewerId);

    /** 终审拒绝：UNDER_REVIEW→REJECTED。 */
    void reject(Long appId, Long reviewerId, String reason);

    /** 打回补充：UNDER_REVIEW→RETURNED。 */
    void returnForSupplement(Long appId, Long reviewerId, String reason);

    // ---- 邀请码 ----

    /** 认证申请前预检邀请码有效性。 */
    InviteCodeCheckDTO checkInviteCode(String code);

    /** 邀请码是否可认领（供内部/M4 复用的布尔判断）。 */
    boolean validateInviteCode(String code);

    /** ADMIN 批量生成毕业生邀请码，返回码列表。 */
    List<String> batchCreateInviteCodes(BatchInviteCodeRequest request);
}
