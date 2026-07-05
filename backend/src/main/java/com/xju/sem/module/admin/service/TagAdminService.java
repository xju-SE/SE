package com.xju.sem.module.admin.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.admin.dto.CreateTagRequest;
import com.xju.sem.module.admin.dto.TagDTO;
import com.xju.sem.module.admin.dto.TagQuery;
import com.xju.sem.module.admin.dto.TagUsageDTO;
import com.xju.sem.module.admin.dto.UpdateTagRequest;

/** 标签体系维护（FR-M7-13/14，07 详细设计 §6.7）。 */
public interface TagAdminService {

    /** 新增标签；命名在同类型同父级下已存在抛 {@code AdminErrorCode.TAG_NAME_DUPLICATE}。 */
    TagDTO create(CreateTagRequest request);

    /** 编辑标签（部分更新）；改名/改父级后仍需满足唯一约束。 */
    TagDTO update(Long id, UpdateTagRequest request);

    /** 停用（软删）标签；被引用中的标签允许停用（前端二次确认，Service 层不阻断，见 §6.7）。 */
    void disable(Long id);

    /** 标签管理列表（含跨表使用计数），FR-M7-14。 */
    PageResult<TagUsageDTO> pageWithUsageCount(TagQuery query);
}
