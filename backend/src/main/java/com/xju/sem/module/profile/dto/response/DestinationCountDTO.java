package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 去向分布单元（§6.3）。一级用于去向类型分布（key=destinationType）；二级下钻用于行业/院校分布
 * （key=分桶标识，小于 k-匿名阈值的桶被合并为 "OTHER"）。
 */
@Data
@Builder
public class DestinationCountDTO {

    /** 分布键：一级为 destination_type；二级为行业标签 id 或院校名，合并桶为 "OTHER"。 */
    private String key;

    /** 可读标签（如去向类型中文名、行业标签名、院校名），供前端直接展示。 */
    private String label;

    private long count;

    /** 占比（0~1，保留两位小数），样本不足时不返回具体分布故不出现。 */
    private Double percentage;
}
