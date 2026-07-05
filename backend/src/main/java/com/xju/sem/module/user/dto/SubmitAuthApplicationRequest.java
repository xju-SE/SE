package com.xju.sem.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 提交认证申请（三条分级路径统一入口，按 verifyMethod 分支）。
 * 各分支所需字段：
 * <ul>
 *   <li>STUDENT_SSO：realName、studentNo、college、majorText（学号命中 mock_student_roster 则自动通过）</li>
 *   <li>STUDENT_MANUAL：realName、studentNo、college、majorText、evidenceUrl（转人工）</li>
 *   <li>ALUMNI_INVITE_CODE：inviteCode（+ realName 核对）</li>
 *   <li>ALUMNI_MANUAL_GUARANTEE：realName、majorText、college、evidenceUrl、guarantor1Id、guarantor2Id</li>
 * </ul>
 */
@Data
public class SubmitAuthApplicationRequest {

    @NotBlank(message = "verifyMethod 不能为空")
    private String verifyMethod;

    private String realName;

    /**
     * 学号：固定 8~12 位纯数字（仅学生认证路径需要）。
     * 校友邀请码/担保路径不传（null 时 @Pattern 跳过），空串同样放行；
     * 一旦提供且格式非法即触发 PARAM_INVALID(20001)。
     */
    @Pattern(regexp = "^(\\d{8,12})?$", message = "学号须为 8~12 位纯数字")
    private String studentNo;
    private String college;
    private String majorText;

    private String evidenceUrl;

    private String inviteCode;

    private Long guarantor1Id;
    private Long guarantor2Id;
}
