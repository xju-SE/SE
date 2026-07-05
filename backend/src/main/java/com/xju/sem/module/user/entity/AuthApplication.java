package com.xju.sem.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 认证申请（表 auth_application）。一条记录覆盖四条分级认证路径与邀请码预生成。
 * 审核类更新一律用状态 CAS（UPDATE ... WHERE status=期望前置状态）防并发重复处理。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auth_application")
public class AuthApplication extends BaseEntity {

    /** 申请人；邀请码预生成（INVITE_ISSUED）阶段为 NULL，认领后回填。 */
    private Long userId;

    /** STUDENT/ALUMNI，申请认证的目标身份。 */
    private String applyRole;

    /** STUDENT_SSO/STUDENT_MANUAL/ALUMNI_INVITE_CODE/ALUMNI_MANUAL_GUARANTEE。 */
    private String verifyMethod;

    private String realName;

    private String studentNo;

    /** 申请填写的专业文本，终审时解析为 major_tag_id。 */
    private String majorText;

    private String college;

    /** 证件/证明材料 URL。 */
    private String evidenceUrl;

    /** 毕业生邀请码（机构侧签发）。 */
    private String inviteCode;

    private Long guarantor1Id;

    private Long guarantor2Id;

    /** 担保人1确认态：PENDING/CONFIRMED/REJECTED（S3 双人担保，持久化半确认）。 */
    private String guarantor1Status;

    /** 担保人2确认态：PENDING/CONFIRMED/REJECTED；两人均 CONFIRMED 才转 UNDER_REVIEW。 */
    private String guarantor2Status;

    /** INVITE_ISSUED/AWAITING_GUARANTEE/PENDING/UNDER_REVIEW/APPROVED/REJECTED/RETURNED/WITHDRAWN/EXPIRED。 */
    private String status;

    /** 是否 SSO/邀请码自动通过：0 否 1 是。 */
    private Integer autoApproved;

    /** 拒绝/退回理由。 */
    private String rejectReason;
}
