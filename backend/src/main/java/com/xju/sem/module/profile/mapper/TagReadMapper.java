package com.xju.sem.module.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.profile.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 全局 tag 表的<b>只读</b> Mapper（本模块仅 select，绝不 insert/update/delete tag）。
 * 用于标签类型校验与标签名回填。待 M7 提供统一 TagQueryService 契约后可替换本 Mapper。
 */
@Mapper
public interface TagReadMapper extends BaseMapper<Tag> {
}
