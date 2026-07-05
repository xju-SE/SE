package com.xju.sem.module.timeline.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.module.timeline.dto.request.CreateTimelineNodeRequest;
import com.xju.sem.module.timeline.dto.request.UpdateTimelineNodeRequest;
import com.xju.sem.module.timeline.dto.response.TimelineNodeDTO;
import com.xju.sem.module.timeline.entity.TimelineNode;
import com.xju.sem.module.timeline.entity.TimelineTemplate;
import com.xju.sem.module.timeline.enums.Stage;
import com.xju.sem.module.timeline.enums.TimelineErrorCode;
import com.xju.sem.module.timeline.mapper.TimelineNodeMapper;
import com.xju.sem.module.timeline.mapper.TimelineTemplateMapper;
import com.xju.sem.module.timeline.service.TimelineNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 时间线节点服务实现：节点 CRUD 与字段范围校验（stage 合法枚举、suggestedMonth∈[1,12]、
 * importance∈[1,3]、orderNo≥0）。节点无独立状态机，可见性跟随所属模板 status（Controller 门控）。
 */
@Service
@RequiredArgsConstructor
public class TimelineNodeServiceImpl implements TimelineNodeService {

    private final TimelineNodeMapper nodeMapper;
    private final TimelineTemplateMapper templateMapper;

    @Override
    public TimelineNodeDTO createNode(Long templateId, Long adminId, CreateTimelineNodeRequest request) {
        requireTemplate(templateId);
        validate(request.getStage(), request.getSuggestedMonth(), request.getImportance(), request.getOrderNo());
        TimelineNode n = new TimelineNode();
        n.setTemplateId(templateId);
        n.setStage(request.getStage());
        n.setTitle(request.getTitle());
        n.setDescription(request.getDescription());
        n.setSuggestedTime(request.getSuggestedTime());
        n.setSuggestedMonth(request.getSuggestedMonth());
        n.setImportance(request.getImportance() == null ? 1 : request.getImportance());
        n.setOrderNo(request.getOrderNo() == null ? 0 : request.getOrderNo());
        nodeMapper.insert(n);
        return TimelineMapping.toNodeDTO(n);
    }

    @Override
    public TimelineNodeDTO updateNode(Long id, Long adminId, UpdateTimelineNodeRequest request) {
        TimelineNode n = requireNode(id);
        validate(request.getStage(), request.getSuggestedMonth(), request.getImportance(), request.getOrderNo());
        n.setStage(request.getStage());
        n.setTitle(request.getTitle());
        n.setDescription(request.getDescription());
        n.setSuggestedTime(request.getSuggestedTime());
        n.setSuggestedMonth(request.getSuggestedMonth());
        n.setImportance(request.getImportance() == null ? 1 : request.getImportance());
        n.setOrderNo(request.getOrderNo() == null ? 0 : request.getOrderNo());
        nodeMapper.updateById(n);
        return TimelineMapping.toNodeDTO(n);
    }

    @Override
    public void deleteNode(Long id, Long adminId) {
        requireNode(id);
        nodeMapper.deleteById(id);
    }

    @Override
    public List<TimelineNodeDTO> listNodesOfTemplate(Long templateId) {
        return nodeMapper.listByTemplate(templateId).stream()
                .sorted(TimelineMapping.NODE_ORDER)
                .map(TimelineMapping::toNodeDTO)
                .toList();
    }

    private void validate(String stage, Integer suggestedMonth, Integer importance, Integer orderNo) {
        if (!Stage.isValid(stage)) {
            throw new BusinessException(TimelineErrorCode.NODE_PARAM_INVALID, "阶段取值不合法");
        }
        if (suggestedMonth != null && (suggestedMonth < 1 || suggestedMonth > 12)) {
            throw new BusinessException(TimelineErrorCode.NODE_PARAM_INVALID, "建议月份需在 1-12 之间");
        }
        if (importance != null && (importance < 1 || importance > 3)) {
            throw new BusinessException(TimelineErrorCode.NODE_PARAM_INVALID, "重要度需在 1-3 之间");
        }
        if (orderNo != null && orderNo < 0) {
            throw new BusinessException(TimelineErrorCode.NODE_PARAM_INVALID, "展示顺序不能为负");
        }
    }

    private void requireTemplate(Long templateId) {
        TimelineTemplate t = templateMapper.selectById(templateId);
        if (t == null) {
            throw new BusinessException(TimelineErrorCode.RESOURCE_NOT_FOUND, "时间线模板不存在或已删除");
        }
    }

    private TimelineNode requireNode(Long id) {
        TimelineNode n = nodeMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(TimelineErrorCode.RESOURCE_NOT_FOUND, "时间线节点不存在或已删除");
        }
        return n;
    }
}
