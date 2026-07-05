package com.xju.sem.module.timeline.service;

import com.xju.sem.module.timeline.dto.request.CreateTimelineNodeRequest;
import com.xju.sem.module.timeline.dto.request.UpdateTimelineNodeRequest;
import com.xju.sem.module.timeline.dto.response.TimelineNodeDTO;

import java.util.List;

/**
 * 时间线节点服务（ADMIN 维护）。节点无独立状态机，可见性完全跟随所属模板 status（非 PUBLISHED
 * 模板下的节点不对学生可见）。{@code listNodesOfTemplate} 返回结果按学期自然序 + orderNo 排序。
 */
public interface TimelineNodeService {

    /** FR-M6-03 新增节点（校验 stage/suggestedMonth/importance/orderNo）。 */
    TimelineNodeDTO createNode(Long templateId, Long adminId, CreateTimelineNodeRequest request);

    /** FR-M6-03 编辑节点。 */
    TimelineNodeDTO updateNode(Long id, Long adminId, UpdateTimelineNodeRequest request);

    /** FR-M6-03 软删除节点。 */
    void deleteNode(Long id, Long adminId);

    /** 取某模板下全部节点（按学期自然序 + orderNo 排序）。ADMIN 任意状态；学生端仅 PUBLISHED 模板可查（Controller 门控）。 */
    List<TimelineNodeDTO> listNodesOfTemplate(Long templateId);
}
