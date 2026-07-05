package com.xju.sem.module.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 编辑标签（FR-M7-13）。字段均可空表示不改动该字段（部分更新）。 */
@Data
public class UpdateTagRequest {

    @Size(max = 50, message = "标签名称不能超过50字")
    private String tagName;

    private Long parentId;

    private Integer sortOrder;
}
