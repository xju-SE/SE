package com.xju.sem.module.opportunity.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** 发布机会（FR-M5-01）。isReferral=true 时校验发布人为已认证 ALUMNI 且 type=INTERNSHIP，见 Service 层。 */
@Data
public class CreateOpportunityRequest {

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

    /** 是否标记为内推类（需 M7 终审通过方可对外公开）。 */
    private Boolean isReferral;

    /** S19：是否允许围绕本机会发起组队；为空/false 时该机会不支持 {@code createTeam} 关联。 */
    private Boolean teamRequired;
}
