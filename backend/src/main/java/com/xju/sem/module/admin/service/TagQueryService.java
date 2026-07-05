package com.xju.sem.module.admin.service;

import com.xju.sem.module.admin.dto.TagDTO;

import java.util.List;

/**
 * 标签只读查询服务（跨模块契约，07 详细设计 §8）：供 M2/M4/M5/M6 等全模块只读依赖，
 * {@code TagMapper} 物理归属本模块，避免各模块各自持有 Mapper 造成维护职责分裂。
 */
public interface TagQueryService {

    /** 按类型列出未停用标签（sortOrder 升序），tagType 为空时返回全部类型。 */
    List<TagDTO> listByType(String tagType);

    /** 按 id 取标签；不存在或已停用返回 null（供调用方自行决定是否报错，不抛异常）。 */
    TagDTO getById(Long id);

    /** 标签是否存在且未停用。 */
    boolean exists(Long id);

    /**
     * 跨模块契约（供 M1/M4/M5 等按"专业文本"解析 major_tag_id 使用）：按 MAJOR 类型标签名精确
     * 匹配，查不到返回 {@code defaultTagId}（可为 null）。与 M1 现有 {@code MajorTagResolver} 内部
     * 直连 {@code MajorTagMapper} 的既有实现并行存在（M1 已落地代码本次不做迁移改造，避免破坏其
     * 已有类），未来 M1 侧可选择迁移到本契约，见实现说明"假设与简化"。
     */
    Long resolveMajorTag(String majorName, Long defaultTagId);
}
