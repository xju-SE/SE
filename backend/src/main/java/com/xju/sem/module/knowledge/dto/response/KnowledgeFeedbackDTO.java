package com.xju.sem.module.knowledge.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 单条三态评价出参（POST /{id}/feedbacks 的返回体）。 */
@Data
@Builder
public class KnowledgeFeedbackDTO {

    private Long id;
    private Long entryId;
    private Long userId;
    private String feedbackType;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
