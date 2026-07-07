package com.xju.sem.module.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发送私信入参。 */
@Data
public class SendMessageRequest {

    @NotNull(message = "receiverId 不能为空")
    private Long receiverId;

    @NotBlank(message = "content 不能为空")
    @Size(max = 2000, message = "私信内容不能超过2000字")
    private String content;
}
