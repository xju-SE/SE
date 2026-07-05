package com.xju.sem.module.timeline.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 节点关联引用出参（"零复制、现取"）：{@code refType/refId} 为本模块持有的引用键；{@code refTitle}/
 * {@code refStatus}/{@code available} 为展示时实时调各模块 {@code getBrief}/{@code existsPublished}/
 * {@code getVisiblePathCard} 取回的摘要，本模块不缓存、不复制被引用对象正文（§9）。
 * {@code available=false} 表示被引用对象已删除/下线/对当前访问者不可见，前端渲染失效占位。
 */
@Data
@Builder
public class TimelineNodeRefDTO {

    private Long nodeId;
    /** ALUMNI_PATH_CARD/KNOWLEDGE_ENTRY/OPPORTUNITY。 */
    private String refType;
    private Long refId;
    /** 实时取回的展示标题（不可用时为 null）。 */
    private String refTitle;
    /** 实时取回的被引用对象状态（如机会的 ONGOING；不适用时为 null）。 */
    private String refStatus;
    /** 被引用对象当前是否可用（存在且对访问者可见）。 */
    private boolean available;
}
