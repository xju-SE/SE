package com.xju.sem.module.admin.dto;

import lombok.Builder;
import lombok.Data;

/** 标签管理列表行（P18 Tab③ 左侧），含跨表只读使用计数（FR-M7-14）。 */
@Data
@Builder
public class TagUsageDTO {
    private Long id;
    private String tagType;
    private String tagName;
    private Long parentId;
    private Integer sortOrder;

    /** 被 user_tag/student_profile/help_ticket 等业务表引用的总次数，见 {@code TagMapper#countUsage}。 */
    private long usageCount;
}
