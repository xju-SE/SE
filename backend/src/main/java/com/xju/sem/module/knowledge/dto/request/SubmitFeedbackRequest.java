package com.xju.sem.module.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 提交/更新三态评价（FR-M3-12）。 */
@Data
public class SubmitFeedbackRequest {

    @NotBlank(message = "评价类型不能为空")
    @Pattern(regexp = "USEFUL|OUTDATED|NEED_UPDATE", message = "评价类型必须是 USEFUL/OUTDATED/NEED_UPDATE 之一")
    private String feedbackType;

    @Size(max = 300, message = "纠错说明不能超过300字")
    private String comment;
}
