package com.xju.sem.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.admin.dto.CreateTagRequest;
import com.xju.sem.module.admin.dto.TagDTO;
import com.xju.sem.module.admin.dto.TagQuery;
import com.xju.sem.module.admin.dto.TagUsageDTO;
import com.xju.sem.module.admin.dto.UpdateTagRequest;
import com.xju.sem.module.admin.entity.Tag;
import com.xju.sem.module.admin.enums.AdminErrorCode;
import com.xju.sem.module.admin.enums.TagType;
import com.xju.sem.module.admin.mapper.TagMapper;
import com.xju.sem.module.admin.service.TagAdminService;
import com.xju.sem.module.admin.service.TagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签体系维护 + 只读查询实现（07 详细设计 §6.7/§8/§9："二者拆分接口但共享同一 TagMapper，
 * 只读接口对外暴露给其余模块、写接口仅本模块内部 Controller 调用"）。
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagQueryService, TagAdminService {

    private final TagMapper tagMapper;

    // ---------------- TagQueryService（只读，跨模块契约） ----------------

    @Override
    public List<TagDTO> listByType(String tagType) {
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(tagType)) {
            qw.eq(Tag::getTagType, tagType);
        }
        qw.orderByAsc(Tag::getSortOrder).orderByAsc(Tag::getId);
        return tagMapper.selectList(qw).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public TagDTO getById(Long id) {
        if (id == null) {
            return null;
        }
        Tag tag = tagMapper.selectById(id);
        return tag == null ? null : toDTO(tag);
    }

    @Override
    public boolean exists(Long id) {
        return id != null && tagMapper.selectById(id) != null;
    }

    @Override
    public Long resolveMajorTag(String majorName, Long defaultTagId) {
        if (!StringUtils.hasText(majorName)) {
            return defaultTagId;
        }
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<>();
        qw.eq(Tag::getTagType, TagType.MAJOR.name()).eq(Tag::getTagName, majorName.trim()).last("LIMIT 1");
        Tag tag = tagMapper.selectOne(qw);
        return tag != null ? tag.getId() : defaultTagId;
    }

    // ---------------- TagAdminService（写操作，仅本模块 Controller 调用） ----------------

    @Override
    public TagDTO create(CreateTagRequest request) {
        if (!TagType.isValid(request.getTagType())) {
            throw new BusinessException(AdminErrorCode.TAG_TYPE_INVALID, "标签类型取值不合法");
        }
        checkDuplicate(request.getTagType(), request.getTagName(), request.getParentId(), null);

        Tag tag = new Tag();
        tag.setTagType(request.getTagType());
        tag.setTagName(request.getTagName());
        tag.setParentId(request.getParentId());
        tag.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        tagMapper.insert(tag);
        return toDTO(tag);
    }

    @Override
    public TagDTO update(Long id, UpdateTagRequest request) {
        Tag tag = requireExisting(id);
        String newName = StringUtils.hasText(request.getTagName()) ? request.getTagName() : tag.getTagName();
        Long newParent = request.getParentId() != null ? request.getParentId() : tag.getParentId();
        if (!newName.equals(tag.getTagName()) || !java.util.Objects.equals(newParent, tag.getParentId())) {
            checkDuplicate(tag.getTagType(), newName, newParent, id);
        }
        tag.setTagName(newName);
        tag.setParentId(newParent);
        if (request.getSortOrder() != null) {
            tag.setSortOrder(request.getSortOrder());
        }
        tagMapper.updateById(tag);
        return toDTO(tag);
    }

    @Override
    public void disable(Long id) {
        requireExisting(id);
        // 停用即软删除（MyBatis-Plus @TableLogic 自动转 UPDATE ... SET deleted=1）；历史引用记录中
        // 的 tag_id 保持不变（不级联清空），前端"该标签仍被N处引用"的二次确认由 §6.7 usageCount
        // 只读展示驱动，Service 层不阻断该操作（见 07 详细设计 §6.7）。
        tagMapper.deleteById(id);
    }

    @Override
    public PageResult<TagUsageDTO> pageWithUsageCount(TagQuery query) {
        if (StringUtils.hasText(query.getTagType()) && !TagType.isValid(query.getTagType())) {
            throw new BusinessException(AdminErrorCode.TAG_TYPE_INVALID, "标签类型取值不合法");
        }
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTagType())) {
            qw.eq(Tag::getTagType, query.getTagType());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            qw.like(Tag::getTagName, query.getKeyword().trim());
        }
        qw.orderByAsc(Tag::getTagType).orderByAsc(Tag::getSortOrder);
        IPage<Tag> page = tagMapper.selectPage(pageOf(query.getPage(), query.getSize()), qw);
        List<TagUsageDTO> records = page.getRecords().stream().map(t -> TagUsageDTO.builder()
                .id(t.getId())
                .tagType(t.getTagType())
                .tagName(t.getTagName())
                .parentId(t.getParentId())
                .sortOrder(t.getSortOrder())
                .usageCount(tagMapper.countUsage(t.getId()))
                .build()).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    // ---------------- helpers ----------------

    /** 校验 uk_type_name_parent(tag_type, tag_name, parent_id) 唯一约束（parentId 为空按 IS NULL 处理）。 */
    private void checkDuplicate(String tagType, String tagName, Long parentId, Long excludeId) {
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<>();
        qw.eq(Tag::getTagType, tagType).eq(Tag::getTagName, tagName);
        if (parentId != null) {
            qw.eq(Tag::getParentId, parentId);
        } else {
            qw.isNull(Tag::getParentId);
        }
        if (excludeId != null) {
            qw.ne(Tag::getId, excludeId);
        }
        if (tagMapper.selectCount(qw) > 0) {
            throw new BusinessException(AdminErrorCode.TAG_NAME_DUPLICATE, "标签命名在同类型同父级下已存在");
        }
    }

    private Tag requireExisting(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "标签不存在");
        }
        return tag;
    }

    private Page<Tag> pageOf(int page, int size) {
        int p = page <= 0 ? 1 : page;
        int s = size <= 0 ? 10 : Math.min(size, 100);
        return new Page<>(p, s);
    }

    private TagDTO toDTO(Tag t) {
        return TagDTO.builder()
                .id(t.getId())
                .tagType(t.getTagType())
                .tagName(t.getTagName())
                .parentId(t.getParentId())
                .sortOrder(t.getSortOrder())
                .build();
    }
}
