package com.xju.sem.module.profile.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 覆盖式更新成长标签入参（PUT /user-tags/me，FR-M2-02）。tagIds 会被去重，
 * 每个标签须为 tag_type∈{INTEREST,GROWTH}，数量上限 10（Service 层校验）。
 */
@Data
public class UpdateUserTagsRequest {

    private List<Long> tagIds;
}
