package com.xju.sem.module.admin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 标签出参（跨模块契约：{@code TagQueryService} 的返回类型，供 M2/M4/M5/M6 等全模块只读依赖）。
 */
@Data
@Builder
public class TagDTO {
    private Long id;
    private String tagType;
    private String tagName;
    private Long parentId;
    private Integer sortOrder;
}
