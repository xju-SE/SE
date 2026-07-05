package com.xju.sem.module.timeline.service.impl;

import com.xju.sem.module.timeline.dto.response.TimelineNodeDTO;
import com.xju.sem.module.timeline.dto.response.TimelineTemplateDTO;
import com.xju.sem.module.timeline.entity.TimelineNode;
import com.xju.sem.module.timeline.entity.TimelineTemplate;
import com.xju.sem.module.timeline.enums.Stage;

import java.util.Comparator;

/**
 * 本模块实体 → DTO 的无状态映射与节点排序工具（包内复用，避免 TimelineNodeService/UserProgressService
 * 重复实现）。节点排序按学期自然序（{@link Stage#getOrder()}）→ orderNo → id，因 stage 为字符串
 * 枚举、非字典序，不能在 SQL 直接 ORDER BY stage。
 */
final class TimelineMapping {

    /** 节点展示排序器：学期自然序 → orderNo → id。 */
    static final Comparator<TimelineNode> NODE_ORDER =
            Comparator.comparingInt((TimelineNode n) -> stageOrder(n.getStage()))
                    .thenComparingInt(n -> n.getOrderNo() == null ? 0 : n.getOrderNo())
                    .thenComparing(TimelineNode::getId);

    private TimelineMapping() {
    }

    static int stageOrder(String stage) {
        Stage s = Stage.from(stage);
        return s == null ? Integer.MAX_VALUE : s.getOrder();
    }

    static String stageLabel(String stage) {
        Stage s = Stage.from(stage);
        return s == null ? stage : s.getLabel();
    }

    static TimelineTemplateDTO toTemplateDTO(TimelineTemplate t) {
        return TimelineTemplateDTO.builder()
                .id(t.getId())
                .majorTagId(t.getMajorTagId())
                .routeType(t.getRouteType())
                .name(t.getName())
                .status(t.getStatus())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    static TimelineNodeDTO toNodeDTO(TimelineNode n) {
        return TimelineNodeDTO.builder()
                .id(n.getId())
                .templateId(n.getTemplateId())
                .stage(n.getStage())
                .stageLabel(stageLabel(n.getStage()))
                .title(n.getTitle())
                .description(n.getDescription())
                .suggestedTime(n.getSuggestedTime())
                .suggestedMonth(n.getSuggestedMonth())
                .importance(n.getImportance())
                .orderNo(n.getOrderNo())
                .build();
    }
}
