package com.xju.sem.module.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.profile.dto.response.TagDTO;
import com.xju.sem.module.profile.entity.UserTag;
import com.xju.sem.module.profile.enums.TagSource;
import com.xju.sem.module.profile.enums.TagType;
import com.xju.sem.module.profile.mapper.UserTagMapper;
import com.xju.sem.module.profile.service.UserTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 成长标签服务实现（FR-M2-02）。覆盖式更新采用"物理删旧 + 批量插新"（user_tag 无 deleted 列）。
 */
@Service
@RequiredArgsConstructor
public class UserTagServiceImpl implements UserTagService {

    /** 成长标签数量上限（§FR-M2-02 / P04 校验规则）。 */
    private static final int MAX_TAGS = 10;

    private final UserTagMapper userTagMapper;
    private final ProfileTagSupport tagSupport;

    @Override
    public List<TagDTO> listUserTags(Long userId) {
        List<UserTag> rows = userTagMapper.selectList(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, userId));
        List<Long> tagIds = new ArrayList<>();
        for (UserTag r : rows) {
            tagIds.add(r.getTagId());
        }
        return tagSupport.loadTagDTOs(tagIds);
    }

    @Override
    @Transactional
    public List<TagDTO> updateMyTags(Long userId, List<Long> tagIds) {
        // 去重（保序）
        Set<Long> distinct = new LinkedHashSet<>();
        if (tagIds != null) {
            for (Long id : tagIds) {
                if (id != null) {
                    distinct.add(id);
                }
            }
        }
        if (distinct.size() > MAX_TAGS) {
            throw new BusinessException(ResultCode.LIMIT_EXCEEDED,
                    "成长标签数量不能超过 " + MAX_TAGS + " 个");
        }
        // 类型校验：仅允许 INTEREST / GROWTH
        tagSupport.requireAllTypes(distinct, TagType.INTEREST.name(), TagType.GROWTH.name());

        // 覆盖式：物理删旧
        userTagMapper.delete(new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, userId));
        // 插新
        for (Long tagId : distinct) {
            UserTag ut = new UserTag();
            ut.setUserId(userId);
            ut.setTagId(tagId);
            ut.setTagSource(TagSource.SELF.name());
            userTagMapper.insert(ut);
        }
        return tagSupport.loadTagDTOs(distinct);
    }
}
