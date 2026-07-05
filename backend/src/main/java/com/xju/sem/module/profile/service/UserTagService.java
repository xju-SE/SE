package com.xju.sem.module.profile.service;

import com.xju.sem.module.profile.dto.response.TagDTO;

import java.util.List;

/**
 * 成长标签服务（FR-M2-02）。STUDENT/ALUMNI 通用：读取/覆盖式更新本人 user_tag。
 * 数量上限与标签类型校验在实现内完成；被 {@code StudentProfileService.listUserTags} 复用。
 */
public interface UserTagService {

    /** 读取某用户的成长标签（tag_type∈{INTEREST,GROWTH}），按 tag.sort_order 排序。 */
    List<TagDTO> listUserTags(Long userId);

    /** 覆盖式更新本人成长标签：去重、上限校验、类型校验、物理删旧插新。返回更新后的标签列表。 */
    List<TagDTO> updateMyTags(Long userId, List<Long> tagIds);
}
