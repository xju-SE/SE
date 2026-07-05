package com.xju.sem.module.knowledge.service;

import com.xju.sem.module.knowledge.dto.response.FeedbackSummaryDTO;
import com.xju.sem.module.knowledge.dto.response.KnowledgeFeedbackDTO;

/** 三态评价/纠错 Service 接口（§6.5）。 */
public interface KnowledgeFeedbackService {

    /** FR-M3-12 提交/更新三态评价，entry_id+user_id 唯一，upsert 语义。 */
    KnowledgeFeedbackDTO submitFeedback(Long entryId, Long userId, String feedbackType, String comment);

    /** FR-M3-13 查看三态评价统计（实时聚合，见实现说明"假设与简化"）。 */
    FeedbackSummaryDTO getSummary(Long entryId, Long viewerUserId);
}
