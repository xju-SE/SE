package com.xju.sem.module.user.service.impl;

import com.xju.sem.module.user.mapper.MajorTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 专业文本 → major_tag_id 的简单解析器（终审/自动通过回写档案时使用）。
 * 按 tag 表 MAJOR 名称精确匹配，查不到给默认标签 id（避免 profile.major_tag_id 非空约束失败）。
 */
@Component
@RequiredArgsConstructor
public class MajorTagResolver {

    private final MajorTagMapper majorTagMapper;

    /** 未匹配时的兜底专业标签 id，可经 sem.auth.default-major-tag-id 配置，缺省 1。 */
    @Value("${sem.auth.default-major-tag-id:1}")
    private Long defaultMajorTagId;

    public Long resolve(String majorText) {
        if (StringUtils.hasText(majorText)) {
            Long id = majorTagMapper.findMajorIdByName(majorText.trim());
            if (id != null) {
                return id;
            }
        }
        return defaultMajorTagId;
    }
}
