package com.xju.sem.module.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.profile.dto.response.TagDTO;
import com.xju.sem.module.profile.entity.Tag;
import com.xju.sem.module.profile.mapper.TagReadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标签只读支撑：类型校验（目标行业须 INDUSTRY、专业须 MAJOR、成长标签须 INTEREST/GROWTH）与
 * 标签名回填。仅 select tag 表，绝不写。待 M7 提供统一 TagQueryService 后可整体替换本类。
 */
@Component
@RequiredArgsConstructor
public class ProfileTagSupport {

    private final TagReadMapper tagReadMapper;

    /** 单个标签的类型；标签不存在返回 null。 */
    public Tag find(Long tagId) {
        return tagId == null ? null : tagReadMapper.selectById(tagId);
    }

    /**
     * 校验标签存在且类型属于允许集合，否则抛 20001。用于目标行业/专业等单标签入参。
     * tagId 为 null 时直接放行（可空字段的清空语义由调用方处理）。
     */
    public void requireType(Long tagId, String... allowedTypes) {
        if (tagId == null) {
            return;
        }
        Tag tag = tagReadMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "标签不存在: " + tagId);
        }
        for (String t : allowedTypes) {
            if (t.equals(tag.getTagType())) {
                return;
            }
        }
        throw new BusinessException(ResultCode.PARAM_INVALID,
                "标签类型非法: " + tagId + " 期望 " + String.join("/", allowedTypes));
    }

    /**
     * 批量校验一组标签均属允许类型，返回按入参顺序的 Tag 列表（供覆盖式写标签时复用查询结果）。
     * 任一标签不存在或类型不符抛 20001。
     */
    public List<Tag> requireAllTypes(Collection<Long> tagIds, String... allowedTypes) {
        List<Tag> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(tagIds)) {
            return result;
        }
        List<Tag> tags = tagReadMapper.selectBatchIds(tagIds);
        Map<Long, Tag> byId = new LinkedHashMap<>();
        for (Tag t : tags) {
            byId.put(t.getId(), t);
        }
        for (Long id : tagIds) {
            Tag tag = byId.get(id);
            if (tag == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "标签不存在: " + id);
            }
            boolean ok = false;
            for (String type : allowedTypes) {
                if (type.equals(tag.getTagType())) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "标签类型非法: " + id + " 期望 " + String.join("/", allowedTypes));
            }
            result.add(tag);
        }
        return result;
    }

    /** 按 id 列表回填 TagDTO（按 sort_order 排序），供画像/推荐展示标签名。 */
    public List<TagDTO> loadTagDTOs(Collection<Long> tagIds) {
        List<TagDTO> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(tagIds)) {
            return result;
        }
        List<Tag> tags = tagReadMapper.selectList(new LambdaQueryWrapper<Tag>()
                .in(Tag::getId, tagIds)
                .orderByAsc(Tag::getSortOrder));
        for (Tag t : tags) {
            result.add(toDTO(t));
        }
        return result;
    }

    /** 单个标签名（用于统计下钻的行业名回显）；不存在返回 null。 */
    public String tagName(Long tagId) {
        Tag t = find(tagId);
        return t == null ? null : t.getTagName();
    }

    public TagDTO toDTO(Tag t) {
        return TagDTO.builder()
                .id(t.getId())
                .tagType(t.getTagType())
                .tagName(t.getTagName())
                .parentId(t.getParentId())
                .build();
    }
}
