package com.xju.sem.module.help.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 求助单详情聚合出参（FR-M4-05）：求助单信息 + 全部回答 + 追问线程 + 当前用户操作位。
 * 追问线程按求助单组织（schema help_followup 无 target_answer_id，为按单线程）。
 */
@Data
public class HelpTicketDetailDTO {

    private HelpTicketDTO ticket;

    private List<HelpAnswerDTO> answers;

    private List<HelpFollowupDTO> followups;

    private MyActionsDTO myActions;
}
