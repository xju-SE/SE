package com.xju.sem.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** {successCount, failCount, details[]}（07 详细设计 §5(b)/§6.9）。 */
@Data
@AllArgsConstructor
public class BatchResultDTO {
    private int successCount;
    private int failCount;
    private List<BatchResultItem> details;
}
