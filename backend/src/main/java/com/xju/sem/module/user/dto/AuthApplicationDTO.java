package com.xju.sem.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 认证申请出参（进度查询 / 提交回执 / M7 审核列表复用）。 */
@Data
public class AuthApplicationDTO {

    private Long id;
    private Long userId;
    private String applyRole;
    private String verifyMethod;
    private String realName;
    private String studentNo;
    private String majorText;
    private String college;
    private String evidenceUrl;
    private String inviteCode;
    private Long guarantor1Id;
    private Long guarantor2Id;
    /** 担保人1/2 确认态：PENDING/CONFIRMED/REJECTED（S3 双人担保）。 */
    private String guarantor1Status;
    private String guarantor2Status;
    private String status;
    private Integer autoApproved;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 供前端展示的状态说明文案（如“已通过/已转人工审核”）。 */
    private String statusHint;
}
