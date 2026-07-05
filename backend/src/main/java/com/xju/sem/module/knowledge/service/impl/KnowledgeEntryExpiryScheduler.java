package com.xju.sem.module.knowledge.service.impl;

import com.xju.sem.module.knowledge.entity.KnowledgeEntry;
import com.xju.sem.module.knowledge.mapper.KnowledgeEntryMapper;
import com.xju.sem.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * FR-M3-16 定时任务：过期自动降权（§6.7），每日 02:30 扫描。
 * PUBLISHED 且 valid_until 已过期的条目 → CAS 转 EXPIRED，并通知认领人/作者。
 *
 * <p><b>"降权"在现有 schema 下的实现方式</b>：03 详细设计的 weight_score 衰减列在
 * schema.sql 的 knowledge_entry 中未开（该表已按 08/09 集成裁决 reconcile，精确列以其为准），
 * 故本实现不做数值衰减，而是通过状态迁移到 EXPIRED——list()/search() 默认只返回 PUBLISHED——
 * 使条目自然退出公共列表/搜索的默认可见范围，效果等价于"降权隐藏"，符合 FR-M3-16 的核心诉求
 * （过期内容不应继续以原有权重呈现）。§6.8（三态反馈驱动降权与预警）、§6.9（长期未维护释放认领）
 * 两个定时任务因分别依赖不存在的 weight_score/claimed_at 列，本期不实现，见实现说明文档
 * "假设与简化"一节的显式范围声明。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeEntryExpiryScheduler {

    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void scanExpiredEntries() {
        List<KnowledgeEntry> candidates = knowledgeEntryMapper.selectExpirable(LocalDate.now());
        int expiredCount = 0;
        for (KnowledgeEntry entry : candidates) {
            // CAS：仅当仍为 PUBLISHED 时才生效，防止与并发的手动下线/编辑重复处理
            int rows = knowledgeEntryMapper.expireIfPublished(entry.getId());
            if (rows != 1) {
                continue;
            }
            expiredCount++;
            Long recipient = entry.getClaimerId() != null ? entry.getClaimerId() : entry.getAuthorId();
            if (recipient != null) {
                try {
                    notificationService.send(recipient, "SYSTEM", "知识条目已过期",
                            "你贡献/认领的知识条目《" + entry.getTitle() + "》已过期，建议更新后重新提交审核",
                            "KNOWLEDGE_ENTRY", entry.getId());
                } catch (Exception e) {
                    log.warn("knowledge_entry {} 过期通知发送失败: {}", entry.getId(), e.getMessage());
                }
            }
            log.info("knowledge_entry {} expired by valid_until, status -> EXPIRED", entry.getId());
        }
        log.info("知识条目过期降权扫描完成：命中候选{}条，实际转EXPIRED{}条", candidates.size(), expiredCount);
    }
}
