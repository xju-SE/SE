package com.xju.sem.module.opportunity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 队伍成员出参（P15 成员列表行）。createdAt/updatedAt 兼任申请时间/最近处理时间语义。 */
@Data
@Builder
public class TeamMemberDTO {

    private Long id;
    private Long teamId;
    private Long userId;
    private String userName;

    /** LEADER/MEMBER。 */
    private String memberRole;

    /** APPLYING/JOINED/REJECTED/LEFT。 */
    private String joinStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
