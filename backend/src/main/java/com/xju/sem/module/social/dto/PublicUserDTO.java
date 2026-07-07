package com.xju.sem.module.social.dto;

import lombok.Data;

import java.util.List;

/**
 * 他人主页公开资料出参。由 {@code ProfileViewMapper} 多表 JOIN 组装，
 * 仅含允许对外展示的字段（不含 real_name/student_no 等强隐私字段）。
 */
@Data
public class PublicUserDTO {

    /** 目标用户 user.id。 */
    private Long userId;

    private String username;

    /** STUDENT/ALUMNI/ADMIN。 */
    private String role;

    /** UNVERIFIED/PENDING/VERIFIED/REJECTED。 */
    private String authStatus;

    /** 个人简介，取 student_profile/alumni_profile 中存在的一份。 */
    private String bio;

    /** 专业名称（tag.tag_name），来自 student_profile/alumni_profile 的 major_tag_id。 */
    private String major;

    /** 年级档（仅 STUDENT 有值，ALUMNI/ADMIN 为 null）。 */
    private Integer grade;

    private String avatarUrl;

    /** 用户自定义/系统标签名称列表。 */
    private List<String> tags;

    /** 粉丝数：被多少人关注。 */
    private long followerCount;

    /** 关注数：关注了多少人。 */
    private long followingCount;

    /** 当前登录用户（查看者）是否已关注目标用户。 */
    private boolean following;

    /** 发布的知识条目 + 求助单总数。 */
    private long postCount;

    /** 公开徽章名称列表（hidden=0）。 */
    private List<String> badges;
}
