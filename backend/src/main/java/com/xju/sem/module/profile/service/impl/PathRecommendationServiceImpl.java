package com.xju.sem.module.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.module.profile.dto.request.PathRecommendRequest;
import com.xju.sem.module.profile.dto.response.MatchedAlumniDTO;
import com.xju.sem.module.profile.dto.response.PathRecommendationDTO;
import com.xju.sem.module.profile.dto.response.RecommendationItemDTO;
import com.xju.sem.module.profile.dto.response.VisiblePathCardDTO;
import com.xju.sem.module.profile.entity.AlumniPathCard;
import com.xju.sem.module.profile.entity.Tag;
import com.xju.sem.module.profile.entity.UserTag;
import com.xju.sem.module.profile.enums.DestinationType;
import com.xju.sem.module.profile.enums.PathCardStatus;
import com.xju.sem.module.profile.enums.TagType;
import com.xju.sem.module.profile.mapper.AlumniPathCardMapper;
import com.xju.sem.module.profile.mapper.TagReadMapper;
import com.xju.sem.module.profile.mapper.UserTagMapper;
import com.xju.sem.module.profile.service.AlumniPathCardService;
import com.xju.sem.module.profile.service.PathRecommendationService;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.entity.AlumniProfile;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 路径推荐规则引擎实现（§6.4）。四层：候选池构建（硬性专业过滤 + 冷启动放宽）→ 打分（软性加权，
 * 不硬剔除避免空结果）→ 多样性去重（同公司/院校仅一次）→ 分去向类型取 TopN + 匹配校友。纯读不落库。
 *
 * <p>与详细设计的适配：冷启动放宽先按"同学院跨专业"收窄（JOIN alumni_profile.college，C16），
 * 仅在无法定位请求者学院或同学院无卡时才退回全平台已发布卡片（限量）；深造意向信号
 * containsInterestSignal 用兴趣标签名关键词启发式判定。
 */
@Service
@RequiredArgsConstructor
public class PathRecommendationServiceImpl implements PathRecommendationService {

    // ---- 打分权重（可后续外置为配置）----
    private static final double W_MAJOR = 5.0;
    private static final double W_INDUSTRY = 3.0;
    private static final double W_CITY = 2.0;
    private static final double W_INTENT = 2.0;
    private static final double W_GPA = 2.0;
    private static final double W_INTEREST = 3.0;      // 乘 jaccard(0..1)
    private static final double W_TRUST_BONUS = 1.5;   // 贡献者信任加权
    private static final double W_TRUST_LOG = 1.0;     // 乘 log(1+adoptedCount)

    private static final int TOP_N = 10;
    private static final int PER_GROUP = 3;
    private static final int COLD_START_LIMIT = 200;

    /** 深造意向关键词（命中兴趣标签名即视为有深造信号）。 */
    private static final List<String> POSTGRAD_KEYWORDS =
            Arrays.asList("考研", "深造", "读研", "读博", "升学", "保研");

    private final AlumniPathCardMapper cardMapper;
    private final UserTagMapper userTagMapper;
    private final TagReadMapper tagReadMapper;
    private final IdentityProfileSupport identitySupport;
    private final UserService userService;
    private final AlumniPathCardService pathCardService;

    @Value("${sem.knowledge.stat-min-sample:20}")
    private int minSample;

    @Override
    public PathRecommendationDTO recommend(Long requesterId, PathRecommendRequest req) {
        // 第一层：候选池（硬性专业过滤），空则冷启动放宽
        List<AlumniPathCard> pool = cardMapper.selectList(new LambdaQueryWrapper<AlumniPathCard>()
                .eq(AlumniPathCard::getStatus, PathCardStatus.PUBLISHED.name())
                .eq(AlumniPathCard::getMajorTagId, req.getMajorTagId()));
        long sameMajorCount = pool.size();
        if (pool.isEmpty()) {
            // C16：冷启动放宽先按"同学院跨专业"收窄（JOIN alumni_profile.college），而非直接全平台放宽。
            String college = identitySupport.collegeOf(requesterId);
            if (StringUtils.hasText(college)) {
                pool = cardMapper.selectPublishedByCollege(college, COLD_START_LIMIT);
            }
            // 学院无法定位或同学院也无卡时，才退回全平台放宽（限量），避免推荐结果彻底为空。
            if (pool.isEmpty()) {
                pool = cardMapper.selectList(new LambdaQueryWrapper<AlumniPathCard>()
                        .eq(AlumniPathCard::getStatus, PathCardStatus.PUBLISHED.name())
                        .last("limit " + COLD_START_LIMIT));
            }
        }
        boolean lowSampleWarning = sameMajorCount < minSample;

        boolean postgradSignal = containsInterestSignal(req.getInterestTagIds());
        Set<Long> inputInterest = toSet(req.getInterestTagIds());
        Map<Long, Set<Long>> ownerInterestCache = new HashMap<>();

        // 第二层：打分（软性加权）
        List<Scored> scoredList = new ArrayList<>();
        for (AlumniPathCard card : pool) {
            List<String> reasons = new ArrayList<>();
            double score = 0;
            if (req.getMajorTagId().equals(card.getMajorTagId())) {
                score += W_MAJOR;
                reasons.add("专业相同");
            }
            if (req.getTargetIndustryTagId() != null && DestinationType.isEmploy(card.getDestinationType())
                    && req.getTargetIndustryTagId().equals(card.getIndustryTagId())) {
                score += W_INDUSTRY;
                reasons.add("行业匹配");
            }
            if (StringUtils.hasText(req.getTargetCity()) && DestinationType.isEmploy(card.getDestinationType())
                    && req.getTargetCity().equals(card.getCity())) {
                score += W_CITY;
                reasons.add("目标城市匹配");
            }
            if (postgradSignal && DestinationType.isPostgrad(card.getDestinationType())) {
                score += W_INTENT;
                reasons.add("深造意向匹配");
            }
            if (req.getGpaRangeMin() != null && req.getGpaRangeMax() != null && card.getGradGpa() != null
                    && card.getGradGpa().compareTo(req.getGpaRangeMin()) >= 0
                    && card.getGradGpa().compareTo(req.getGpaRangeMax()) <= 0) {
                score += W_GPA;
                reasons.add("GPA相近");
            }
            double overlap = jaccard(inputInterest,
                    ownerInterestCache.computeIfAbsent(card.getUserId(), this::interestTagsOf));
            if (overlap > 0) {
                score += W_INTEREST * overlap;
                reasons.add("兴趣重合");
            }
            AlumniProfile ap = identitySupport.findAlumniProfile(card.getUserId());
            if (ap != null && ap.getIsContributorBadge() != null && ap.getIsContributorBadge() == 1) {
                score += W_TRUST_BONUS;
                reasons.add("贡献者");
            }
            int adopted = ap == null || ap.getAdoptedCount() == null ? 0 : ap.getAdoptedCount();
            score += W_TRUST_LOG * Math.log1p(adopted);

            scoredList.add(new Scored(card, score, reasons));
        }

        // 第三层：排序 + 多样性去重（同公司/院校最多一次）
        scoredList.sort((a, b) -> Double.compare(b.score, a.score));
        List<Scored> diversified = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Scored s : scoredList) {
            String key = dedupKey(s.card);
            if (key != null && !seen.add(key)) {
                continue;
            }
            diversified.add(s);
            if (diversified.size() >= TOP_N) {
                break;
            }
        }

        // 第四层：按去向类型分组，每组取前 PER_GROUP，保证多样展示
        List<Scored> grouped = groupByDestinationTakeTop(diversified);

        List<RecommendationItemDTO> recommendations = new ArrayList<>();
        Map<Long, MatchedAlumniDTO> matched = new LinkedHashMap<>();
        for (Scored s : grouped) {
            VisiblePathCardDTO summary = pathCardService.getVisiblePathCard(s.card.getId(), requesterId);
            recommendations.add(RecommendationItemDTO.builder()
                    .pathCardSummary(summary)
                    .matchScore(round2(s.score))
                    .matchReasons(s.reasons)
                    .build());
            matched.computeIfAbsent(s.card.getUserId(), this::buildMatchedAlumni);
        }

        return PathRecommendationDTO.builder()
                .recommendations(recommendations)
                .matchedAlumni(new ArrayList<>(matched.values()))
                .lowSampleWarning(lowSampleWarning)
                .build();
    }

    // ==================== 打分辅助 ====================

    /** 兴趣标签名命中深造关键词即视为有深造意向信号。 */
    private boolean containsInterestSignal(List<Long> interestTagIds) {
        if (CollectionUtils.isEmpty(interestTagIds)) {
            return false;
        }
        List<Tag> tags = tagReadMapper.selectBatchIds(interestTagIds);
        for (Tag t : tags) {
            String name = t.getTagName();
            if (name == null) {
                continue;
            }
            for (String kw : POSTGRAD_KEYWORDS) {
                if (name.contains(kw)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 某校友的 INTEREST 类型自选标签集合（供 jaccard 计算，带缓存）。 */
    private Set<Long> interestTagsOf(Long userId) {
        List<UserTag> rows = userTagMapper.selectList(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, userId));
        if (rows.isEmpty()) {
            return new HashSet<>();
        }
        List<Long> ids = new ArrayList<>();
        for (UserTag r : rows) {
            ids.add(r.getTagId());
        }
        Set<Long> interest = new HashSet<>();
        for (Tag t : tagReadMapper.selectBatchIds(ids)) {
            if (TagType.INTEREST.name().equals(t.getTagType())) {
                interest.add(t.getId());
            }
        }
        return interest;
    }

    private double jaccard(Set<Long> a, Set<Long> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0;
        }
        int inter = 0;
        for (Long x : a) {
            if (b.contains(x)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0 : (double) inter / union;
    }

    private String dedupKey(AlumniPathCard card) {
        if (DestinationType.isEmploy(card.getDestinationType())) {
            return card.getCompany() == null ? null : "C:" + card.getCompany();
        }
        if (DestinationType.isPostgrad(card.getDestinationType())) {
            return card.getTargetSchool() == null ? null : "S:" + card.getTargetSchool();
        }
        return null; // 其他去向不参与去重
    }

    private List<Scored> groupByDestinationTakeTop(List<Scored> ranked) {
        Map<String, Integer> perGroupCount = new HashMap<>();
        List<Scored> out = new ArrayList<>();
        for (Scored s : ranked) {
            String g = s.card.getDestinationType();
            int c = perGroupCount.getOrDefault(g, 0);
            if (c >= PER_GROUP) {
                continue;
            }
            perGroupCount.put(g, c + 1);
            out.add(s);
        }
        return out;
    }

    private MatchedAlumniDTO buildMatchedAlumni(Long userId) {
        AlumniProfile ap = identitySupport.findAlumniProfile(userId);
        UserBriefDTO ub = userService.getBrief(userId);
        return MatchedAlumniDTO.builder()
                .userId(userId)
                .nickname(ub == null ? null : ub.getUsername())
                .badge(ap == null ? null : ap.getIsContributorBadge())
                .adoptedCount(ap == null ? null : ap.getAdoptedCount())
                .avatarUrl(ap == null ? (ub == null ? null : ub.getAvatarUrl()) : ap.getAvatarUrl())
                .build();
    }

    private Set<Long> toSet(List<Long> ids) {
        Set<Long> set = new HashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null) {
                    set.add(id);
                }
            }
        }
        return set;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** 打分中间体（不落库）。 */
    private static final class Scored {
        final AlumniPathCard card;
        final double score;
        final List<String> reasons;

        Scored(AlumniPathCard card, double score, List<String> reasons) {
            this.card = card;
            this.score = score;
            this.reasons = reasons;
        }
    }
}
