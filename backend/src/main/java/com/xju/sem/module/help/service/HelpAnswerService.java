package com.xju.sem.module.help.service;

import com.xju.sem.module.help.dto.AnswerContentDTO;
import com.xju.sem.module.help.dto.request.SubmitAnswerRequest;
import com.xju.sem.module.help.dto.response.HelpAnswerDTO;

/**
 * 回答服务：模板化回答提交/编辑、采纳、以及供 §6.2 打分与 M3 采纳生成候选复用的读方法。
 *
 * <p>{@link #getForCandidate(Long)} 与 {@link #countAdopted(Long, Long)} 为跨模块契约方法，
 * 签名不可变更：前者被 M3 {@code KnowledgeEntryService.createFromHelpAdoption} 依赖，
 * 后者被本模块 {@code HelpRouteService} 路由打分复用。
 */
public interface HelpAnswerService {

    /** FR-M4-06 提交三段式回答；首条回答触发 MATCHED/OPEN→ANSWERED，通知求助人。 */
    HelpAnswerDTO submitAnswer(Long ticketId, Long responderId, SubmitAnswerRequest request);

    /** FR-M4-07 编辑本人回答（未被采纳前）。 */
    HelpAnswerDTO editAnswer(Long answerId, Long operatorId, SubmitAnswerRequest request);

    /**
     * FR-M4-10 采纳最佳回答：置 is_adopted，ANSWERED→ADOPTED（CAS），发
     * {@code HelpAnswerAdoptedEvent}（AFTER_COMMIT 触发 M3 候选生成与回写）。
     */
    void adopt(Long ticketId, Long answerId, Long operatorId);

    /**
     * 跨模块契约：供 M3 采纳生成知识候选时自读回答正文（precondition/steps/cautions/ticketTitle）。
     * answerId 不存在返回 null（由调用方按不存在处理）。
     */
    AnswerContentDTO getForCandidate(Long answerId);

    /**
     * 供 §6.2 路由打分调用：某回答人被采纳次数；questionTypeTagId 为 null 统计全部，否则限定该问题类型。
     */
    int countAdopted(Long responderId, Long questionTypeTagId);
}
