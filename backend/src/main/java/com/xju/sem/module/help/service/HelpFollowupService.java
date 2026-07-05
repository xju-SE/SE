package com.xju.sem.module.help.service;

import com.xju.sem.module.help.dto.response.HelpFollowupDTO;

import java.util.List;

/**
 * 追问服务：限次追问（FR-M4-08）与回答人回复（FR-M4-09）。
 * 限次口径：仅求助人追问计入 help_ticket.followup_count，达 {@code sem.help.followup-limit} 抛 LIMIT_EXCEEDED；
 * 回答人回复不计次数。
 */
public interface HelpFollowupService {

    /** 提交追问/回复：按当前用户是求助人还是回答人自动判定并限次。 */
    HelpFollowupDTO submitFollowup(Long ticketId, Long senderId, String content);

    /** 查看某求助单的追问线程（时间正序）。 */
    List<HelpFollowupDTO> listFollowups(Long ticketId, Long viewerId);
}
