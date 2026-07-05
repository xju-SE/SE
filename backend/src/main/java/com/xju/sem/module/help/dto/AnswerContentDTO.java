package com.xju.sem.module.help.dto;

import lombok.Data;

/**
 * 回答正文素材（跨模块契约 DTO）。供 M3 {@code KnowledgeEntryService.createFromHelpAdoption}
 * 在采纳后自读回答正文、拼装知识候选正文使用。
 *
 * <p>包路径与字段访问方式（getTicketTitle/getPrecondition/getSteps/getCautions）已被
 * {@code com.xju.sem.module.knowledge.service.impl.KnowledgeEntryServiceImpl} 依赖，不可变更。
 * 其中 {@code steps} 为渲染后的带序号纯文本（而非数组），便于 M3 直接拼进正文。
 */
@Data
public class AnswerContentDTO {

    /** 来源求助单标题，作知识候选标题的默认来源。 */
    private String ticketTitle;

    /** 适用前提，可空。 */
    private String precondition;

    /** 操作步骤（已渲染为"1. …\n2. …"带序号纯文本）。 */
    private String steps;

    /** 注意事项，可空。 */
    private String cautions;
}
