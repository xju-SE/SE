package com.xju.sem.module.admin.dto;

import com.xju.sem.module.knowledge.dto.response.KnowledgeEntryDTO;
import com.xju.sem.module.opportunity.dto.response.OpportunityDTO;
import com.xju.sem.module.profile.dto.response.AlumniBriefDTO;
import com.xju.sem.module.user.dto.AuthApplicationDTO;
import com.xju.sem.module.user.dto.UserBriefDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核任务详情（P18 Tab① 详情弹层）。target_type 对应的目标详情字段二选一非空：
 * AUTH_APPLICATION→{@link #authApplication}；KNOWLEDGE_ENTRY→{@link #knowledgeEntry}；
 * OPPORTUNITY→{@link #opportunity}；CONTRIBUTOR_CERT→{@link #contributorApplicant} +
 * {@link #contributorCertPayload}（M7 剩余部分补充后三者）。
 */
@Data
@Builder
public class AuditTaskDetailDTO {
    private Long id;
    private String targetType;
    private Long targetId;
    private String reviewKind;
    private String status;
    private Long submitterId;
    private UserBriefDTO submitter;
    private Long reviewerId;
    private String decisionNote;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;

    /** 仅 KNOWLEDGE_ENTRY 有意义值；供详情页预填"三秒可判断" checklist 提示。 */
    private PreCheckResultDTO preCheck;

    private AuthApplicationDTO authApplication;
    private KnowledgeEntryDTO knowledgeEntry;

    /** 仅 OPPORTUNITY 有意义值（FR-M7-08 终审详情）。 */
    private OpportunityDTO opportunity;

    /** 仅 CONTRIBUTOR_CERT 有意义值：申请人已帮助/被采纳计数摘要（P18 Tab④ 展示，FR-M7-19）。 */
    private AlumniBriefDTO contributorApplicant;

    /** 仅 CONTRIBUTOR_CERT 有意义值：申请材料（荣誉证明 URL + 说明），见 {@link ContributorCertPayload}。 */
    private ContributorCertPayload contributorCertPayload;
}
