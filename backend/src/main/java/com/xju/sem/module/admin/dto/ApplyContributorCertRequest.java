package com.xju.sem.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /api/v1/contributor-cert-applications 请求体（FR-M7-18）。 */
@Data
public class ApplyContributorCertRequest {

    @NotBlank(message = "荣誉证明附件不能为空")
    @Size(max = 255, message = "荣誉证明附件地址过长")
    private String honorCertUrl;

    @Size(max = 300, message = "申请说明不能超过300字")
    private String note;
}
