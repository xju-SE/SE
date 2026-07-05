package com.xju.sem.module.admin.controller;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.Result;
import com.xju.sem.module.admin.dto.CreateTagRequest;
import com.xju.sem.module.admin.dto.TagDTO;
import com.xju.sem.module.admin.dto.TagQuery;
import com.xju.sem.module.admin.dto.TagUsageDTO;
import com.xju.sem.module.admin.dto.UpdateTagRequest;
import com.xju.sem.module.admin.service.TagAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标签体系维护（P18 Tab③ 左侧，FR-M7-13/14）。全部接口仅 ADMIN 可用。
 */
@RestController
@RequestMapping("/api/v1/admin/tags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TagAdminController {

    private final TagAdminService tagAdminService;

    /** FR-M7-14 标签管理列表（含使用计数）。 */
    @GetMapping
    public Result<PageResult<TagUsageDTO>> page(
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        TagQuery query = new TagQuery();
        query.setTagType(tagType);
        query.setKeyword(keyword);
        query.setPage(page);
        query.setSize(size);
        return Result.ok(tagAdminService.pageWithUsageCount(query));
    }

    /** FR-M7-13 新增标签。 */
    @PostMapping
    public Result<TagDTO> create(@Valid @RequestBody CreateTagRequest request) {
        return Result.ok(tagAdminService.create(request));
    }

    /** FR-M7-13 编辑标签（部分更新）。 */
    @PutMapping("/{id}")
    public Result<TagDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateTagRequest request) {
        return Result.ok(tagAdminService.update(id, request));
    }

    /** FR-M7-13 停用（软删）标签。 */
    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        tagAdminService.disable(id);
        return Result.ok();
    }
}
