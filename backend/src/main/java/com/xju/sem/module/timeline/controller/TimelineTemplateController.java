package com.xju.sem.module.timeline.controller;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.common.security.SecurityUtil;
import com.xju.sem.module.timeline.dto.request.CreateTimelineNodeRequest;
import com.xju.sem.module.timeline.dto.request.CreateTimelineTemplateRequest;
import com.xju.sem.module.timeline.dto.request.OfflineTemplateRequest;
import com.xju.sem.module.timeline.dto.request.TimelineTemplateQuery;
import com.xju.sem.module.timeline.dto.request.UpdateTimelineTemplateRequest;
import com.xju.sem.module.timeline.dto.response.TimelineNodeDTO;
import com.xju.sem.module.timeline.dto.response.TimelineTemplateDTO;
import com.xju.sem.module.timeline.service.TimelineNodeService;
import com.xju.sem.module.timeline.service.TimelineTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 时间线模板维护 Controller（P18 管理后台，ADMIN）。模板由 ADMIN 直接维护并自助发布，不经 M7
 * 审核队列（§1/§9）——故权限注解为 {@code hasRole('ADMIN')}，挂本模块自身路由，仅做入参校验转发。
 * 节点新增/查看内联于模板下（RESTful 从属资源），节点编辑/删除/引用见 {@link TimelineNodeController}。
 */
@RestController
@RequestMapping("/api/v1/timeline-templates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TimelineTemplateController {

    private final TimelineTemplateService templateService;
    private final TimelineNodeService nodeService;

    /** 模板列表（按 major/route/status 过滤）。 */
    @GetMapping
    public Result<PageResult<TimelineTemplateDTO>> page(
            @RequestParam(required = false) Long majorTagId,
            @RequestParam(required = false) String routeType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        TimelineTemplateQuery query = new TimelineTemplateQuery();
        query.setMajorTagId(majorTagId);
        query.setRouteType(routeType);
        query.setStatus(status);
        query.setPage(page);
        query.setSize(size);
        return Result.ok(templateService.page(query));
    }

    /** 模板详情（ADMIN 可见任意状态）。 */
    @GetMapping("/{id}")
    public Result<TimelineTemplateDTO> getById(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    /** 新建模板（默认 DRAFT）。 */
    @PostMapping
    public Result<TimelineTemplateDTO> create(@Valid @RequestBody CreateTimelineTemplateRequest request) {
        return Result.ok(templateService.create(SecurityUtil.currentUserId(), request));
    }

    /** 编辑模板基本信息。 */
    @PutMapping("/{id}")
    public Result<TimelineTemplateDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateTimelineTemplateRequest request) {
        return Result.ok(templateService.update(id, SecurityUtil.currentUserId(), request));
    }

    /** 发布：DRAFT/OFFLINE → PUBLISHED。 */
    @PatchMapping("/{id}/publish")
    public Result<TimelineTemplateDTO> publish(@PathVariable Long id) {
        return Result.ok(templateService.publish(id, SecurityUtil.currentUserId()));
    }

    /** 下线：PUBLISHED → OFFLINE。 */
    @PatchMapping("/{id}/offline")
    public Result<TimelineTemplateDTO> offline(@PathVariable Long id,
                                               @RequestBody(required = false) OfflineTemplateRequest request) {
        String reason = request == null ? null : request.getReason();
        return Result.ok(templateService.offline(id, SecurityUtil.currentUserId(), reason));
    }

    /** 软删除模板。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id, SecurityUtil.currentUserId());
        return Result.ok();
    }

    /** 查看模板节点列表（按学期自然序 + orderNo）。 */
    @GetMapping("/{id}/nodes")
    public Result<List<TimelineNodeDTO>> listNodes(@PathVariable Long id) {
        return Result.ok(nodeService.listNodesOfTemplate(id));
    }

    /** 在模板下新增节点。 */
    @PostMapping("/{id}/nodes")
    public Result<TimelineNodeDTO> createNode(@PathVariable Long id,
                                              @Valid @RequestBody CreateTimelineNodeRequest request) {
        return Result.ok(nodeService.createNode(id, SecurityUtil.currentUserId(), request));
    }
}
