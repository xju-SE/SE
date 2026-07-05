package com.xju.sem.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 全局标签（表 {@code tag}，字段定义见 02 详细设计 §3.3；表主体归属全局基础表，但维护端点
 * （增删改）与 {@code TagMapper} 物理归属本模块——M2 §3.3/07 详细设计 §3.3 已明确"维护端点见
 * M7 详细设计"）。含逻辑删除与审计时间（继承 {@link BaseEntity}）。
 *
 * <p>{@code parentId} 支持简单两级层级（如 GRADE 下细分档位），非叶子结构不强校验层数。
 * 唯一约束 {@code uk_type_name_parent(tag_type, tag_name, parent_id)} 由 DB 兜底，Service
 * 层新增/编辑前先做只读校验给出更友好的错误提示（见 {@code AdminErrorCode.TAG_NAME_DUPLICATE}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
public class Tag extends BaseEntity {

    /** MAJOR/GRADE/INDUSTRY/INTEREST/GROWTH/QUESTION_TYPE，见 {@code TagType}。 */
    private String tagType;

    private String tagName;

    /** 父标签 id，NULL 表示顶级标签。 */
    private Long parentId;

    /** 同级展示顺序，越小越靠前。 */
    private Integer sortOrder;
}
