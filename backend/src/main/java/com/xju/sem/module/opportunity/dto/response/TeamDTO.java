package com.xju.sem.module.opportunity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 队伍详情出参（P15）。members 仅详情接口携带（队长额外可见 APPLYING 待审批申请）。 */
@Data
@Builder
public class TeamDTO {

    private Long id;
    private Long opportunityId;
    private String opportunityTitle;
    private Long leaderId;
    private String leaderName;
    private String title;
    private String description;
    private String needDesc;
    private Integer capacity;
    private Integer currentSize;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TeamMemberDTO> members;

    /** 当前查看者是否为队长（前端据此展示审批区/状态操作按钮）。 */
    private boolean isLeader;

    /** 当前查看者是否可申请加入（登录、非队长、当前无有效申请、队伍招募中）。 */
    private boolean joinable;
}
