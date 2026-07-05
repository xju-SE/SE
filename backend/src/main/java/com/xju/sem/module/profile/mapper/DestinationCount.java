package com.xju.sem.module.profile.mapper;

import lombok.Data;

/**
 * 去向聚合投影行（非持久化实体）。既用于一级去向类型分布，也复用于二级维度下钻
 * （bucketKey 承载 industry_tag_id / target_school 等分桶键，见 §6.3）。
 */
@Data
public class DestinationCount {

    /** 一级：destination_type 枚举名；二级下钻：分桶键（行业标签 id / 院校名，可能为 null）。 */
    private String bucketKey;

    /** 该桶记录数。 */
    private Long cnt;
}
