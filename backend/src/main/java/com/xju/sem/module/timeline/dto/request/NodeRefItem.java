package com.xju.sem.module.timeline.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 覆盖式更新节点引用的单条（FR-M6-04）。schema timeline_node_ref 无 ref_order 列，展示顺序取
 * 请求列表的自然顺序，故本项不含 refOrder（相对详细设计的裁剪，见实现说明"假设与简化"）。
 */
@Data
public class NodeRefItem {

    @NotBlank(message = "引用类型不能为空")
    private String refType;

    @NotNull(message = "引用对象ID不能为空")
    private Long refId;
}
