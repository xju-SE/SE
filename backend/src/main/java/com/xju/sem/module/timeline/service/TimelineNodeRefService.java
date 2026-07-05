package com.xju.sem.module.timeline.service;

import com.xju.sem.module.timeline.dto.request.NodeRefItem;
import com.xju.sem.module.timeline.dto.response.TimelineNodeRefDTO;

import java.util.List;

/**
 * 节点关联引用服务（只存 ID、现取展示、覆盖式维护）。
 *
 * <p><b>对外跨模块契约</b>：{@link #countByRef} 供 M2/M3/M5 详情页展示"被 N 个成长时间线节点引用"
 * （签名与地基契约"M6(提供,供 M3 P09 计数)：long TimelineNodeRefService.countByRef(String, Long)"
 * 严格一致，例如 M3 P09"该知识被 N 个时间线节点引用"，§6.7）。其余方法供 ADMIN 维护与展示装配。
 */
public interface TimelineNodeRefService {

    /** 取某节点全部引用并现取各模块摘要（viewer 由当前登录态推断，用于路径卡按访问者脱敏）。 */
    List<TimelineNodeRefDTO> listRefs(Long nodeId);

    /** FR-M6-04 覆盖式更新某节点引用：逐条按 refType 校验存在性后物理删旧插新。 */
    List<TimelineNodeRefDTO> replaceRefs(Long nodeId, Long adminId, List<NodeRefItem> refs);

    /**
     * 跨模块契约（供 M2/M3/M5 详情页）：统计引用某对象的节点引用条数。
     * 本模块只做只读聚合，不感知调用方展示逻辑（§6.7）。
     */
    long countByRef(String refType, Long refId);
}
