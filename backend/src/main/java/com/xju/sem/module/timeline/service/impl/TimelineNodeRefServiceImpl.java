package com.xju.sem.module.timeline.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.security.LoginUser;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.knowledge.dto.response.KnowledgeBriefDTO;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import com.xju.sem.module.opportunity.dto.response.OpportunityBriefDTO;
import com.xju.sem.module.opportunity.service.OpportunityService;
import com.xju.sem.module.profile.dto.response.VisiblePathCardDTO;
import com.xju.sem.module.profile.service.AlumniPathCardService;
import com.xju.sem.module.timeline.dto.request.NodeRefItem;
import com.xju.sem.module.timeline.dto.response.TimelineNodeRefDTO;
import com.xju.sem.module.timeline.entity.TimelineNode;
import com.xju.sem.module.timeline.entity.TimelineNodeRef;
import com.xju.sem.module.timeline.enums.RefType;
import com.xju.sem.module.timeline.enums.TimelineErrorCode;
import com.xju.sem.module.timeline.mapper.TimelineNodeMapper;
import com.xju.sem.module.timeline.mapper.TimelineNodeRefMapper;
import com.xju.sem.module.timeline.service.TimelineNodeRefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 节点关联引用服务实现："只存 ID + 现取展示 + 覆盖式维护"。展示与存在性校验一律通过 M2/M3/M5
 * 各自 Service 接口（不直连其 Mapper/entity），保持地基"跨模块只调 Service"的低耦合约定（§9）。
 * 展示时对每条引用的摘要取回做 try/catch 兜底：任一被引用对象异常/失效只把该条标记为不可用，
 * 不影响聚合视图整体渲染（引用是"最佳努力"的现取展示）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineNodeRefServiceImpl implements TimelineNodeRefService {

    private final TimelineNodeRefMapper refMapper;
    private final TimelineNodeMapper nodeMapper;
    private final AlumniPathCardService alumniPathCardService;
    private final KnowledgeEntryService knowledgeEntryService;
    private final OpportunityService opportunityService;

    @Override
    public List<TimelineNodeRefDTO> listRefs(Long nodeId) {
        requireNode(nodeId);
        Long viewerId = currentViewerId();
        List<TimelineNodeRefDTO> result = new ArrayList<>();
        for (TimelineNodeRef ref : refMapper.listByNode(nodeId)) {
            result.add(resolve(ref, viewerId));
        }
        return result;
    }

    @Override
    @Transactional
    public List<TimelineNodeRefDTO> replaceRefs(Long nodeId, Long adminId, List<NodeRefItem> refs) {
        requireNode(nodeId);
        List<NodeRefItem> items = refs == null ? List.of() : refs;
        // 逐条校验 + 请求内去重（避免撞 uk_node_ref）
        Set<String> seen = new LinkedHashSet<>();
        List<NodeRefItem> deduped = new ArrayList<>();
        for (NodeRefItem item : items) {
            if (!RefType.isValid(item.getRefType())) {
                throw new BusinessException(TimelineErrorCode.NODE_PARAM_INVALID, "引用类型取值不合法");
            }
            if (item.getRefId() == null) {
                throw new BusinessException(TimelineErrorCode.NODE_PARAM_INVALID, "引用对象ID不能为空");
            }
            if (!refExists(item.getRefType(), item.getRefId())) {
                throw new BusinessException(TimelineErrorCode.RESOURCE_NOT_FOUND,
                        "引用对象不存在或不可引用：" + item.getRefType() + "#" + item.getRefId());
            }
            if (seen.add(item.getRefType() + "#" + item.getRefId())) {
                deduped.add(item);
            }
        }
        // 覆盖式重建：物理删旧插新（该表无 deleted 列）
        refMapper.deleteByNode(nodeId);
        for (NodeRefItem item : deduped) {
            TimelineNodeRef ref = new TimelineNodeRef();
            ref.setNodeId(nodeId);
            ref.setRefType(item.getRefType());
            ref.setRefId(item.getRefId());
            refMapper.insert(ref);
        }
        return listRefs(nodeId);
    }

    @Override
    public long countByRef(String refType, Long refId) {
        if (refType == null || refId == null) {
            return 0L;
        }
        return refMapper.countByRef(refType, refId);
    }

    /** 存在性校验：按 refType 路由到对应模块的只读 Service。 */
    private boolean refExists(String refType, Long refId) {
        try {
            return switch (RefType.valueOf(refType)) {
                case ALUMNI_PATH_CARD -> alumniPathCardService.existsPathCard(refId);
                case KNOWLEDGE_ENTRY -> knowledgeEntryService.existsPublished(refId);
                case OPPORTUNITY -> opportunityService.getBrief(refId) != null;
            };
        } catch (RuntimeException e) {
            log.warn("引用存在性校验异常 refType={} refId={}: {}", refType, refId, e.getMessage());
            return false;
        }
    }

    /** 现取被引用对象摘要（零复制）。任一异常/失效 → available=false，不中断聚合视图。 */
    private TimelineNodeRefDTO resolve(TimelineNodeRef ref, Long viewerId) {
        TimelineNodeRefDTO.TimelineNodeRefDTOBuilder b = TimelineNodeRefDTO.builder()
                .nodeId(ref.getNodeId())
                .refType(ref.getRefType())
                .refId(ref.getRefId())
                .available(false);
        try {
            switch (RefType.valueOf(ref.getRefType())) {
                case ALUMNI_PATH_CARD -> {
                    VisiblePathCardDTO card = alumniPathCardService.getVisiblePathCard(ref.getRefId(), viewerId);
                    if (card != null) {
                        String title = (card.getOwnerNickname() == null ? "校友" : card.getOwnerNickname())
                                + (card.getDestinationType() == null ? "" : " · " + card.getDestinationType());
                        b.available(true).refTitle(title).refStatus(card.getStatus());
                    }
                }
                case KNOWLEDGE_ENTRY -> {
                    KnowledgeBriefDTO brief = knowledgeEntryService.getBrief(ref.getRefId());
                    if (brief != null) {
                        boolean published = "PUBLISHED".equals(brief.getStatus());
                        b.available(published).refTitle(brief.getTitle()).refStatus(brief.getStatus());
                    }
                }
                case OPPORTUNITY -> {
                    OpportunityBriefDTO brief = opportunityService.getBrief(ref.getRefId());
                    if (brief != null) {
                        b.available(true).refTitle(brief.getTitle()).refStatus(brief.getStatus());
                    }
                }
            }
        } catch (RuntimeException e) {
            log.warn("引用摘要现取异常 refType={} refId={}: {}", ref.getRefType(), ref.getRefId(), e.getMessage());
        }
        return b.build();
    }

    private Long currentViewerId() {
        LoginUser lu = SecurityUtil.currentOrNull();
        return lu == null ? null : lu.getUserId();
    }

    private TimelineNode requireNode(Long nodeId) {
        TimelineNode n = nodeMapper.selectById(nodeId);
        if (n == null) {
            throw new BusinessException(TimelineErrorCode.RESOURCE_NOT_FOUND, "时间线节点不存在或已删除");
        }
        return n;
    }
}
