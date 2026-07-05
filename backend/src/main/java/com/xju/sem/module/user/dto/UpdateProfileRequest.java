package com.xju.sem.module.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改基本信息入参。
 * <p>说明：schema 的 user 表不含 nickname/phone 列，可编辑的展示型字段（头像、简介）
 * 落在 profile 上，故本请求作用于当前用户的 student_profile/alumni_profile。
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;

    @Size(max = 500, message = "简介不超过500字")
    private String bio;
}
