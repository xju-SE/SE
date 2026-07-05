package com.xju.sem.module.opportunity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 机会详情出参（P14）。 */
@Data
@Builder
public class OpportunityDTO {

    private Long id;
    private String type;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String status;
    private Long publisherId;
    private String publisherName;
    private Boolean isReferral;

    /** S19：是否允许围绕本机会发起组队（team_required=1 时前端才展示"发起队伍"入口）。 */
    private Boolean teamRequired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 当前查看者是否可编辑（发布人本人且非 ENDED，或 ADMIN）。 */
    private boolean editable;

    /** 当前查看者是否可删除（发布人本人或 ADMIN）。 */
    private boolean deletable;

    /** status=PENDING_REVIEW 时前端展示"审核中，通过后对外可见"提示。 */
    private boolean pendingReview;
}
