package com.xju.sem.module.profile.service;

import com.xju.sem.module.profile.dto.request.PathRecommendRequest;
import com.xju.sem.module.profile.dto.response.PathRecommendationDTO;

/**
 * 路径推荐规则引擎（FR-M2-09，§6.4）。输入专业/年级/GPA区间/兴趣标签/目标城市行业 →
 * 候选池构建 → 打分 → 多样性去重 → 分去向类型取 TopN + 匹配校友。纯读、不落库。
 */
public interface PathRecommendationService {

    /**
     * @param requesterId 发起推荐的学生 user.id（用于对推荐卡做按访问者脱敏）
     * @param request     推荐条件
     */
    PathRecommendationDTO recommend(Long requesterId, PathRecommendRequest request);
}
