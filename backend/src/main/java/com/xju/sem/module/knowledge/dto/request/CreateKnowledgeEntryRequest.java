package com.xju.sem.module.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 创建原创知识条目（FR-M3-01）。category=NAV 时 externalUrl 必填，其余类目必须为空，见 Service 层校验。 */
@Data
public class CreateKnowledgeEntryRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 150, message = "标题长度不能超过150字")
    private String title;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "正文不能为空")
    private String content;

    @Size(max = 200, message = "适用范围描述不能超过200字")
    private String applicableScope;

    private LocalDate validUntil;

    @Size(max = 255, message = "外链地址不能超过255字")
    private String externalUrl;
}
