package com.xju.sem.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 批量操作单条结果（07 详细设计 §6.9）。 */
@Data
@AllArgsConstructor
public class BatchResultItem {
    private Long id;
    private boolean success;
    private String message;
}
