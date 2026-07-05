package com.xju.sem.module.help.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 提交追问/回复入参（FR-M4-08 / FR-M4-09），sender_role 由后端按当前用户身份自动判定。 */
@Data
public class SubmitFollowupRequest {

    @NotBlank(message = "追问内容不能为空")
    @Size(max = 500, message = "追问内容不能超过500字")
    private String content;
}
