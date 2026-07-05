package com.xju.sem.module.user.dto;

import lombok.Data;

/**
 * 用户摘要（跨模块契约）：供 M7 审核列表、担保候选人选择器、M4/M5 展示调用方使用。
 * 只暴露弱隐私摘要，realName 仅在授权场景（审核/担保）透出。
 */
@Data
public class UserBriefDTO {
    private Long userId;
    private String username;
    private String role;
    private String authStatus;
    private String realName;
    private Long majorTagId;
    private String avatarUrl;
}
