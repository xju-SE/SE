package com.xju.sem.module.profile.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 全局标签（表 tag）——本模块<b>只读</b>投影。tag 是跨模块共享基础表，写维护（增删改）归属 M7；
 * 本实体仅供 M2 做"标签类型校验/标签名回填"等只读用途，不提供任何写路径。
 *
 * <p>受控跨模块只读说明：系统尚无统一 TagQueryService 契约（M7 剩余），故本模块与
 * {@code module.user.MajorTagResolver} 一样，自建只读 tag 访问（见实现说明 §4），
 * 待 M7 提供 TagQueryService 后可平滑替换本实体+Mapper。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
public class Tag extends BaseEntity {

    /** MAJOR/GRADE/INDUSTRY/INTEREST/GROWTH/QUESTION_TYPE（见 {@code TagType}）。 */
    private String tagType;

    private String tagName;

    private Long parentId;

    private Integer sortOrder;
}
