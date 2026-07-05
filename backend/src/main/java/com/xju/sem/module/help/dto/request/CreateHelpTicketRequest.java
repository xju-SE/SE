package com.xju.sem.module.help.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布求助单入参（FR-M4-01）。专业/年级不在入参——由 Service 从发布人档案（M2/M1）只读快照写入。
 */
@Data
public class CreateHelpTicketRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 150, message = "标题长度不能超过150字")
    private String title;

    @NotBlank(message = "问题详情不能为空")
    private String content;

    /** 问题类型标签 FK→tag.id (tag_type=QUESTION_TYPE)，必选。 */
    @NotNull(message = "问题类型不能为空")
    private Long questionTypeTagId;

    /** 目标方向（考研/就业/竞赛...），自由文本，可选。 */
    @Size(max = 50, message = "目标方向长度不能超过50字")
    private String targetDirection;
}
