package com.xju.sem.module.opportunity.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** 编辑机会（FR-M5-02）。ENDED 状态不可编辑，见 Service 层校验。 */
@Data
public class UpdateOpportunityRequest {

    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "标题不能为空")
    @Size(max = 150, message = "标题长度不能超过150字")
    private String title;

    @Size(max = 4000, message = "详情正文过长")
    private String description;

    @NotNull(message = "截止时间不能为空")
    @Future(message = "截止时间需晚于当前时间")
    private LocalDateTime deadline;

    private Boolean isReferral;

    /** S19：是否允许围绕本机会发起组队；为空/false 时该机会不支持 {@code createTeam} 关联。 */
    private Boolean teamRequired;
}
