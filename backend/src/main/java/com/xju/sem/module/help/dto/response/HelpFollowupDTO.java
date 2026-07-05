package com.xju.sem.module.help.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/** 追问/回复出参。isAsker 便于前端区分"求助人追问"与"回答人回复"气泡。 */
@Data
public class HelpFollowupDTO {

    private Long id;
    private Long ticketId;
    private Long fromUserId;
    private String fromUserName;
    /** 该条是否由求助人发出（true=追问，false=回答方回复）。 */
    private Boolean isAsker;
    private String content;
    private LocalDateTime createdAt;
}
