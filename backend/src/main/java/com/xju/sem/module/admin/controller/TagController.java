package com.xju.sem.module.admin.controller;

import com.xju.sem.common.result.Result;
import com.xju.sem.module.admin.dto.TagDTO;
import com.xju.sem.module.admin.service.TagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签只读查询（供各模块表单下拉选择使用）。全角色可访问，含未登录（GET 已在
 * {@code SecurityConfig} 白名单放行 {@code /api/v1/tags/**}），07 详细设计 §5(b)。
 */
@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagQueryService tagQueryService;

    /** GET /api/v1/tags?tagType= ：按类型列出未停用标签，tagType 为空返回全部。 */
    @GetMapping("/api/v1/tags")
    public Result<List<TagDTO>> list(@RequestParam(required = false) String tagType) {
        return Result.ok(tagQueryService.listByType(tagType));
    }
}
