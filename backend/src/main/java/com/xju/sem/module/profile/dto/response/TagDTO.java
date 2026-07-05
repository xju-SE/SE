package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 标签摘要（跨模块契约）：{@code StudentProfileService.listUserTags(Long)} 的返回元素，
 * 供 M4 求助-校友路由按标签匹配、以及本模块画像/推荐回填标签名使用。
 */
@Data
@Builder
public class TagDTO {

    private Long id;

    /** MAJOR/GRADE/INDUSTRY/INTEREST/GROWTH/QUESTION_TYPE。 */
    private String tagType;

    private String tagName;

    private Long parentId;
}
