package com.xju.sem.module.profile.service.impl;

import com.xju.sem.module.profile.dto.response.DestinationCountDTO;
import com.xju.sem.module.profile.dto.response.MajorDestinationStatsDTO;
import com.xju.sem.module.profile.enums.DestinationType;
import com.xju.sem.module.profile.mapper.AlumniPathCardMapper;
import com.xju.sem.module.profile.mapper.DestinationCount;
import com.xju.sem.module.profile.service.MajorDestinationStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 去向聚合统计实现（§6.3）。最低样本门槛保护 total &lt; minSample 时不返回任何具体计数/百分比；
 * 二级维度下钻对小于 k-匿名阈值的分桶合并 "OTHER"。聚合口径基于全部 PUBLISHED 记录，
 * 与字段级可见性无关（可见性只控展示给谁，不改变统计分母/分子，见 §3.5 统计口径说明）。
 */
@Service
@RequiredArgsConstructor
public class MajorDestinationStatsServiceImpl implements MajorDestinationStatsService {

    /** 二级维度 k-匿名阈值（比一级门槛更严格），小于此值的桶合并 OTHER。 */
    private static final long K_ANON = 5;
    private static final String OTHER = "OTHER";
    private static final String DIM_INDUSTRY = "INDUSTRY";
    private static final String DIM_SCHOOL = "SCHOOL";

    private final AlumniPathCardMapper cardMapper;
    private final ProfileTagSupport tagSupport;

    /** 最低样本门槛，配置项 sem.knowledge.stat-min-sample，缺省 20。 */
    @Value("${sem.knowledge.stat-min-sample:20}")
    private int minSample;

    @Override
    public MajorDestinationStatsDTO getStats(Long majorTagId) {
        List<DestinationCount> rows = cardMapper.countByDestinationType(majorTagId);
        long total = sum(rows);
        if (total < minSample) {
            return lowSample(majorTagId, total);
        }
        List<DestinationCountDTO> distribution = new ArrayList<>();
        for (DestinationCount r : rows) {
            distribution.add(DestinationCountDTO.builder()
                    .key(r.getBucketKey())
                    .label(r.getBucketKey())
                    .count(r.getCnt())
                    .percentage(round2(r.getCnt(), total))
                    .build());
        }
        return MajorDestinationStatsDTO.builder()
                .majorTagId(majorTagId)
                .totalCount(total)
                .sampleSufficient(true)
                .distribution(distribution)
                .build();
    }

    @Override
    public MajorDestinationStatsDTO getStats(Long majorTagId, String destinationType, String dimension) {
        MajorDestinationStatsDTO base = getStats(majorTagId);
        if (!base.isSampleSufficient()) {
            return base; // 一级样本不足则不下钻
        }
        List<DestinationCount> subRows = null;
        boolean isIndustry = DIM_INDUSTRY.equals(dimension) && DestinationType.isEmploy(destinationType);
        boolean isSchool = DIM_SCHOOL.equals(dimension) && DestinationType.isPostgrad(destinationType);
        if (isIndustry) {
            subRows = cardMapper.countEmployByIndustry(majorTagId);
        } else if (isSchool) {
            subRows = cardMapper.countPostgradBySchool(majorTagId);
        }
        if (subRows == null) {
            return base; // 不支持的维度组合：仅返回一级分布
        }
        base.setSubDistribution(kAnonymize(subRows, isIndustry));
        return base;
    }

    /** k-匿名：小于阈值的桶合并为 OTHER，其余按桶计数；行业桶回填标签名。 */
    private List<DestinationCountDTO> kAnonymize(List<DestinationCount> subRows, boolean industryLabel) {
        long subTotal = sum(subRows);
        long merged = 0;
        List<DestinationCountDTO> result = new ArrayList<>();
        for (DestinationCount r : subRows) {
            if (r.getCnt() < K_ANON || r.getBucketKey() == null) {
                merged += r.getCnt();
                continue;
            }
            String label = r.getBucketKey();
            if (industryLabel) {
                String name = tagSupport.tagName(safeLong(r.getBucketKey()));
                if (name != null) {
                    label = name;
                }
            }
            result.add(DestinationCountDTO.builder()
                    .key(r.getBucketKey())
                    .label(label)
                    .count(r.getCnt())
                    .percentage(round2(r.getCnt(), subTotal))
                    .build());
        }
        if (merged > 0) {
            result.add(DestinationCountDTO.builder()
                    .key(OTHER).label(OTHER)
                    .count(merged)
                    .percentage(round2(merged, subTotal))
                    .build());
        }
        return result;
    }

    private MajorDestinationStatsDTO lowSample(Long majorTagId, long total) {
        return MajorDestinationStatsDTO.builder()
                .majorTagId(majorTagId)
                .totalCount(total)
                .sampleSufficient(false)
                .message("样本不足，仅供参考")
                .distribution(Collections.emptyList())
                .build();
    }

    private long sum(List<DestinationCount> rows) {
        long t = 0;
        for (DestinationCount r : rows) {
            if (r.getCnt() != null) {
                t += r.getCnt();
            }
        }
        return t;
    }

    private Double round2(long count, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(count * 1.0 / total * 100.0) / 100.0;
    }

    private Long safeLong(String s) {
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
