package com.xju.sem.module.opportunity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 队伍摘要出参（P15 组队广场列表行 / 我发起加入的队伍列表行）。 */
@Data
@Builder
public class TeamBriefDTO {

    private Long id;
    private Long opportunityId;
    private String opportunityTitle;
    private Long leaderId;
    private String leaderName;
    private String title;
    private String needDesc;
    private Integer capacity;
    private Integer currentSize;
    private String status;
    private LocalDateTime createdAt;
}
