package com.xju.sem.module.user.dto;

import lombok.Data;

/** RETURNED 状态下补充材料后重新提交（可编辑字段随原路径）。 */
@Data
public class ResubmitAuthApplicationRequest {
    private String realName;
    private String studentNo;
    private String college;
    private String majorText;
    private String evidenceUrl;
    private Long guarantor1Id;
    private Long guarantor2Id;
}
