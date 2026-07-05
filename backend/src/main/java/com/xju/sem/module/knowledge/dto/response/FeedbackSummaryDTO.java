package com.xju.sem.module.knowledge.dto.response;

import lombok.Builder;
import lombok.Data;

/** 三态评价统计（FR-M3-13）。 */
@Data
@Builder
public class FeedbackSummaryDTO {

    private long usefulCount;
    private long outdatedCount;
    private long needUpdateCount;

    /** 当前登录用户已提交的评价类型；未登录或未评价为 null。 */
    private String myFeedbackType;
}
