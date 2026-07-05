package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 按专业聚合去向统计（跨模块契约）：{@code AlumniPathCardService.getMajorDestinationStats(majorTagId)}
 * 的返回类型，供双圈首页仪表盘"本专业去向速览"与 P06 统计卡片使用。
 *
 * <p>最低样本门槛（§6.3）：total &lt; sem.knowledge.stat-min-sample(默认20) 时
 * {@code sampleSufficient=false}、{@code distribution} 为空、仅给 message="样本不足，仅供参考"，
 * <b>不返回任何具体计数/百分比</b>，防止用极小分母反推个体。
 */
@Data
@Builder
public class MajorDestinationStatsDTO {

    private Long majorTagId;

    private long totalCount;

    private boolean sampleSufficient;

    /** 样本不足时的降级提示；样本充足时为 null。 */
    private String message;

    /** 一级去向类型分布；样本不足时为空列表。 */
    private List<DestinationCountDTO> distribution;

    /** 可选二级维度下钻（行业/院校分布，k-匿名合并小桶）；未请求下钻时为 null。 */
    private List<DestinationCountDTO> subDistribution;
}
