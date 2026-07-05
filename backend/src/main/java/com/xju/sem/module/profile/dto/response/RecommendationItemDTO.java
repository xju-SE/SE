package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 单条推荐路径（§6.4）。pathCardSummary 已按请求学生身份脱敏（复用 {@link VisiblePathCardDTO}）；
 * matchScore 为规则引擎打分，matchReasons 为命中的可读理由标签（如"专业相同""行业匹配""GPA相近"）。
 */
@Data
@Builder
public class RecommendationItemDTO {

    private VisiblePathCardDTO pathCardSummary;

    private double matchScore;

    private List<String> matchReasons;
}
