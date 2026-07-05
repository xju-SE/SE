package com.xju.sem.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增标签（FR-M7-13）。 */
@Data
public class CreateTagRequest {

    @NotBlank(message = "标签类型不能为空")
    private String tagType;

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称不能超过50字")
    private String tagName;

    /** 父标签 id，可空（顶级标签）。 */
    private Long parentId;

    private Integer sortOrder;
}
