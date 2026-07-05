package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 路径推荐总出参（POST /path-recommendations，§6.4）。纯读计算、不落库。
 * recommendations 已做同公司/院校去重并按去向类型分组取 TopN；matchedAlumni 为去重后的匹配校友。
 */
@Data
@Builder
public class PathRecommendationDTO {

    private List<RecommendationItemDTO> recommendations;

    private List<MatchedAlumniDTO> matchedAlumni;

    /** 候选池同专业样本不足门槛（复用 §6.3 口径）时置 true，前端提示"仅供参考"。 */
    private boolean lowSampleWarning;
}
