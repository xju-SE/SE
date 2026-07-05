package com.xju.sem.module.timeline.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.timeline.dto.request.ReplaceNodeRefsRequest;
import com.xju.sem.module.timeline.dto.request.UpdateTimelineNodeRequest;
import com.xju.sem.module.timeline.dto.response.TimelineNodeDTO;
import com.xju.sem.module.timeline.dto.response.TimelineNodeRefDTO;
import com.xju.sem.module.timeline.service.TimelineNodeRefService;
import com.xju.sem.module.timeline.service.TimelineNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 时间线节点与引用维护 Controller（P18 管理后台，ADMIN）。节点编辑/删除与引用覆盖式更新。
 * 引用列表 GET 供 ADMIN 维护态回显（现取各模块摘要）；学生端只经聚合视图/路线预览间接读取。
 */
@RestController
@RequestMapping("/api/v1/timeline-nodes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TimelineNodeController {

    private final TimelineNodeService nodeService;
    private final TimelineNodeRefService nodeRefService;

    /** 编辑节点。 */
    @PutMapping("/{id}")
    public Result<TimelineNodeDTO> update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateTimelineNodeRequest request) {
        return Result.ok(nodeService.updateNode(id, SecurityUtil.currentUserId(), request));
    }

    /** 软删除节点。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        nodeService.deleteNode(id, SecurityUtil.currentUserId());
        return Result.ok();
    }

    /** 查看节点关联引用（现取各模块摘要）。 */
    @GetMapping("/{id}/refs")
    public Result<List<TimelineNodeRefDTO>> listRefs(@PathVariable Long id) {
        return Result.ok(nodeRefService.listRefs(id));
    }

    /** 覆盖式更新节点关联引用。 */
    @PutMapping("/{id}/refs")
    public Result<List<TimelineNodeRefDTO>> replaceRefs(@PathVariable Long id,
                                                        @Valid @RequestBody ReplaceNodeRefsRequest request) {
        return Result.ok(nodeRefService.replaceRefs(id, SecurityUtil.currentUserId(), request.getRefs()));
    }
}
