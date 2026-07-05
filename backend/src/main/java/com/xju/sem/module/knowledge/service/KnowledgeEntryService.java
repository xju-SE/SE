package com.xju.sem.module.knowledge.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.knowledge.dto.request.CreateKnowledgeEntryRequest;
import com.xju.sem.module.knowledge.dto.request.OfflineRequest;
import com.xju.sem.module.knowledge.dto.request.UpdateKnowledgeEntryRequest;
import com.xju.sem.module.knowledge.dto.response.KnowledgeBriefDTO;
import com.xju.sem.module.knowledge.dto.response.KnowledgeEntryDTO;

/**
 * 知识条目 Service 接口。跨模块契约方法（createFromHelpAdoption/approve/returnToCandidate/
 * getBrief/existsPublished）签名与地基契约严格一致，供 M4/M6/M7 调用；其余为本模块 Controller
 * 内部使用的方法，签名可自由演进，不受跨模块约束。
 */
public interface KnowledgeEntryService {

    /** FR-M3-01 创建原创知识条目。 */
    KnowledgeEntryDTO create(Long authorId, CreateKnowledgeEntryRequest request);

    /**
     * FR-M3-02 采纳生成知识候选（供 M4 HelpAnswerService.adopt() 在其
     * {@code @TransactionalEventListener(AFTER_COMMIT)} 内调用，独立事务，失败走补偿，
     * 不回滚 M4 的采纳动作，见 09 设计修订说明 R3）。
     * 内部自读 M4 {@code HelpAnswerService.getForCandidate(answerId)} 拼装正文，
     * 建 CANDIDATE 后自动 submitForReview 并发布 {@link com.xju.sem.module.knowledge.event.KnowledgeEntrySubmittedEvent}。
     *
     * @return 生成的 entryId，供 M4 回写 help_answer.knowledge_entry_id
     */
    Long createFromHelpAdoption(Long helpTicketId, Long helpAnswerId, Long authorId);

    /** FR-M3-11 查看详情；非 PUBLISHED 仅作者/认领人/ADMIN 可见，否则按资源不存在处理。 */
    KnowledgeEntryDTO getById(Long id, Long viewerUserId, boolean viewerIsAdmin);

    /** 供 M6/M7 等跨模块只读引用的轻量摘要（契约方法，类型/签名不可变更）。 */
    KnowledgeBriefDTO getBrief(Long id);

    /** 供 M6 等跨模块判断引用目标是否已发布可展示（契约方法）。 */
    boolean existsPublished(Long id);

    /** FR-M3-09 分类筛选列表；仅返回 PUBLISHED，登录用户额外可见本人贡献/认领的其他状态条目。 */
    PageResult<KnowledgeBriefDTO> list(String category, Long viewerUserId, int page, int size);

    /** FR-M3-10 全文搜索（FULLTEXT ngram，短关键词兜底 LIKE，见 §6.6）。 */
    PageResult<KnowledgeBriefDTO> search(String keyword, String category, int page, int size);

    /** FR-M3-14 我的知识贡献列表（author_id=me OR claimer_id=me）。 */
    PageResult<KnowledgeBriefDTO> pageMine(Long userId, String status, int page, int size);

    /** FR-M3-03 编辑（含对已发布内容发起修订），version 做乐观锁校验，见 §6.2。 */
    KnowledgeEntryDTO update(Long id, Long userId, boolean isAdmin, UpdateKnowledgeEntryRequest request);

    /** FR-M3-04 提交审核：CANDIDATE/EXPIRED/OFFLINE → REVIEWING，事务提交后发布提交事件。 */
    KnowledgeEntryDTO submitForReview(Long id, Long userId, boolean isAdmin);

    /** FR-M3-05 知识候选终审通过（契约方法，供 M7 AdminKnowledgeEntryController 调用）。 */
    void approve(Long entryId, Long reviewerId);

    /** FR-M3-06 知识候选终审退回（契约方法，供 M7 AdminKnowledgeEntryController 调用）。 */
    void returnToCandidate(Long entryId, Long reviewerId, String reason);

    /** FR-M3-08 内容认领更新，见 §6.4（本期不含 60 天超时抢占/自动释放，见实现说明简化说明）。 */
    KnowledgeEntryDTO claim(Long id, Long userId);

    /** FR-M3-07 手动下线；ADMIN 可强制下线任意状态，作者/认领人限 PUBLISHED/EXPIRED。 */
    KnowledgeEntryDTO offline(Long id, Long operatorId, boolean isAdmin, OfflineRequest request);

    /** FR-M3-15 软删除；作者仅可删自己贡献的非 PUBLISHED 条目，ADMIN 任意状态可删。 */
    void delete(Long id, Long operatorId, boolean isAdmin);
}
