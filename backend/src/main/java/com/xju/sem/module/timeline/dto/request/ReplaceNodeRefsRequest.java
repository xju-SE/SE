package com.xju.sem.module.timeline.dto.request;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 覆盖式更新某节点的全部关联引用（FR-M6-04）。空列表等价于清空该节点引用。 */
@Data
public class ReplaceNodeRefsRequest {

    @Valid
    private List<NodeRefItem> refs = new ArrayList<>();
}
