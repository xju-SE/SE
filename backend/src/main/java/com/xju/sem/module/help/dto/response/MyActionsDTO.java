package com.xju.sem.module.help.dto.response;

import lombok.Data;

/**
 * 详情页当前用户可执行操作位（FR-M4-05）。前端据此控制"提交回答/追问/采纳"入口的可见与可用。
 * 后端仍在各写接口二次校验，这里只作 UI 提示。
 */
@Data
public class MyActionsDTO {

    /** 是否可提交回答（已认证、非求助人本人、未答过、状态可答）。 */
    private Boolean canAnswer;

    /** 是否可发起追问/回复（参与者、未达追问上限、非终态）。 */
    private Boolean canFollowUp;

    /** 是否可采纳（求助人本人且状态=ANSWERED）。 */
    private Boolean canAdopt;
}
