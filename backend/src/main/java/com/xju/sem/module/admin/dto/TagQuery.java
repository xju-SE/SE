package com.xju.sem.module.admin.dto;

import lombok.Data;

/** FR-M7-14 标签管理列表查询条件。 */
@Data
public class TagQuery {
    private String tagType;
    private String keyword;
    private int page = 1;
    private int size = 10;
}
