package com.xju.sem.module.help.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/** 路由匹配记录出参（诊断/复盘，FR-M4-02 结果的可视化，仅求助人/ADMIN 可查）。 */
@Data
public class HelpRouteDTO {

    private Long id;
    private Long ticketId;
    private Long matchedUserId;
    private String matchedUserName;
    private Integer matchScore;
    private String status;
    private LocalDateTime notifiedAt;
}
