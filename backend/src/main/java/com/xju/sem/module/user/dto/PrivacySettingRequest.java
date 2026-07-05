package com.xju.sem.module.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 隐私设置入参：联系方式/画像可见范围。 */
@Data
public class PrivacySettingRequest {

    @Pattern(regexp = "SELF|SAME_MAJOR|PUBLIC", message = "contactVisibility 取值非法")
    private String contactVisibility;

    @Pattern(regexp = "SELF|SAME_MAJOR|PUBLIC", message = "profileVisibility 取值非法")
    private String profileVisibility;
}
