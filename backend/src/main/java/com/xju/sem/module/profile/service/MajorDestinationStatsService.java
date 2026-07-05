package com.xju.sem.module.profile.service;

import com.xju.sem.module.profile.dto.response.MajorDestinationStatsDTO;

/**
 * 按专业去向聚合统计服务（FR-M2-08，§6.3）。含最低样本门槛保护与二级维度 k-匿名。
 * 纯读操作；本期直接查询，数据量增大后可加 {@code @Scheduled} 预计算缓存（预留任务位）。
 */
public interface MajorDestinationStatsService {

    /** 跨模块可复用（供双圈首页仪表盘）：一级去向类型分布，带样本门槛降级。 */
    MajorDestinationStatsDTO getStats(Long majorTagId);

    /**
     * 带二级维度下钻的统计：dimension=INDUSTRY（EMPLOY 行业分布）或 SCHOOL（POSTGRAD 院校分布），
     * 小于 k-匿名阈值（5）的分桶合并为 "OTHER"。样本不足时同样降级、不返回任何分布。
     */
    MajorDestinationStatsDTO getStats(Long majorTagId, String destinationType, String dimension);
}
