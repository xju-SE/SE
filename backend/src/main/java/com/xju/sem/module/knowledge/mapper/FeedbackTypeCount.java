package com.xju.sem.module.knowledge.mapper;

import lombok.Data;

/** {@link KnowledgeFeedbackMapper#countByType} 的按类型分组计数投影行，非持久化实体。 */
@Data
public class FeedbackTypeCount {
    private String feedbackType;
    private Long cnt;
}
