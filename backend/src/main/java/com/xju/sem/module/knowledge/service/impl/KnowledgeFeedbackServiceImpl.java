package com.xju.sem.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.knowledge.dto.response.FeedbackSummaryDTO;
import com.xju.sem.module.knowledge.dto.response.KnowledgeFeedbackDTO;
import com.xju.sem.module.knowledge.entity.KnowledgeEntry;
import com.xju.sem.module.knowledge.entity.KnowledgeFeedback;
import com.xju.sem.module.knowledge.enums.FeedbackType;
import com.xju.sem.module.knowledge.enums.KnowledgeEntryStatus;
import com.xju.sem.module.knowledge.mapper.FeedbackTypeCount;
import com.xju.sem.module.knowledge.mapper.KnowledgeEntryMapper;
import com.xju.sem.module.knowledge.mapper.KnowledgeFeedbackMapper;
import com.xju.sem.module.knowledge.service.KnowledgeFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 三态评价 upsert 与统计（§6.5）。
 *
 * <p>与 03 详细设计的差异：schema.sql 的 knowledge_entry 未开 useful_count/outdated_count/
 * need_update_count 冗余计数列，故不做"SQL级+1原子更新计数列"，改为 {@link #getSummary} 时对
 * knowledge_feedback 按 feedback_type 分组做实时聚合查询——数据量级（单条目评价数）在课程项目
 * 范围内可忽略性能代价，同时天然避免了冗余计数列可能出现的漂移（drift）问题，见实现说明
 * "假设与简化"一节。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeFeedbackServiceImpl implements KnowledgeFeedbackService {

    private final KnowledgeFeedbackMapper feedbackMapper;
    private final KnowledgeEntryMapper entryMapper;

    @Override
    @Transactional
    public KnowledgeFeedbackDTO submitFeedback(Long entryId, Long userId, String feedbackType, String comment) {
        if (!FeedbackType.isValid(feedbackType)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "评价类型不合法");
        }
        KnowledgeEntry entry = entryMapper.selectById(entryId);
        if (entry == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        String status = entry.getStatus();
        if (!KnowledgeEntryStatus.PUBLISHED.name().equals(status) && !KnowledgeEntryStatus.EXPIRED.name().equals(status)) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅曾公开过的内容开放评价");
        }

        KnowledgeFeedback existing = feedbackMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeFeedback>()
                        .eq(KnowledgeFeedback::getEntryId, entryId)
                        .eq(KnowledgeFeedback::getUserId, userId));
        if (existing == null) {
            KnowledgeFeedback fb = new KnowledgeFeedback();
            fb.setEntryId(entryId);
            fb.setUserId(userId);
            fb.setFeedbackType(feedbackType);
            fb.setComment(comment);
            try {
                feedbackMapper.insert(fb);
                return toDTO(fb);
            } catch (DuplicateKeyException e) {
                // 并发下同一用户两个请求同时首次提交，UK(entry_id,user_id) 命中唯一约束，
                // 退化为更新语义（见 §6.5 upsert）
                KnowledgeFeedback raced = feedbackMapper.selectOne(
                        new LambdaQueryWrapper<KnowledgeFeedback>()
                                .eq(KnowledgeFeedback::getEntryId, entryId)
                                .eq(KnowledgeFeedback::getUserId, userId));
                if (raced == null) {
                    throw e;
                }
                raced.setFeedbackType(feedbackType);
                raced.setComment(comment);
                feedbackMapper.updateById(raced);
                return toDTO(raced);
            }
        }
        existing.setFeedbackType(feedbackType);
        existing.setComment(comment);
        feedbackMapper.updateById(existing);
        return toDTO(existing);
    }

    @Override
    public FeedbackSummaryDTO getSummary(Long entryId, Long viewerUserId) {
        List<FeedbackTypeCount> counts = feedbackMapper.countByType(entryId);
        String mine = null;
        if (viewerUserId != null) {
            KnowledgeFeedback existing = feedbackMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeFeedback>()
                            .eq(KnowledgeFeedback::getEntryId, entryId)
                            .eq(KnowledgeFeedback::getUserId, viewerUserId));
            mine = existing == null ? null : existing.getFeedbackType();
        }
        return FeedbackSummaryDTO.builder()
                .usefulCount(countOf(counts, FeedbackType.USEFUL))
                .outdatedCount(countOf(counts, FeedbackType.OUTDATED))
                .needUpdateCount(countOf(counts, FeedbackType.NEED_UPDATE))
                .myFeedbackType(mine)
                .build();
    }

    private long countOf(List<FeedbackTypeCount> counts, FeedbackType type) {
        return counts.stream()
                .filter(c -> type.name().equals(c.getFeedbackType()))
                .map(FeedbackTypeCount::getCnt)
                .findFirst()
                .orElse(0L);
    }

    private KnowledgeFeedbackDTO toDTO(KnowledgeFeedback fb) {
        return KnowledgeFeedbackDTO.builder()
                .id(fb.getId())
                .entryId(fb.getEntryId())
                .userId(fb.getUserId())
                .feedbackType(fb.getFeedbackType())
                .comment(fb.getComment())
                .createdAt(fb.getCreatedAt())
                .updatedAt(fb.getUpdatedAt())
                .build();
    }
}
