package com.xju.sem.module.profile.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 路径推荐入参（POST /path-recommendations，FR-M2-09）。专业必填（前端默认带入认证专业，不可清空）；
 * 其余条件均可选，留空时规则引擎自动放宽对应过滤/打分层（不做硬性剔除，避免结果为空，§6.4）。
 */
@Data
public class PathRecommendRequest {

    @NotNull(message = "专业必填")
    private Long majorTagId;

    /** 年级档（1..10），可选。 */
    private Integer gradeLevel;

    private BigDecimal gpaRangeMin;

    private BigDecimal gpaRangeMax;

    /** 兴趣标签（tag_type∈{INTEREST,GROWTH}），可选。 */
    private List<Long> interestTagIds;

    private String targetCity;

    private Long targetIndustryTagId;
}
