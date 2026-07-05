package com.xju.sem.module.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 编辑知识条目（FR-M3-03），version 用于乐观锁校验（见实现说明"并发控制"）。 */
@Data
public class UpdateKnowledgeEntryRequest {

    @NotNull(message = "version 不能为空")
    private Integer version;

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
