package com.xju.sem.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 邀请码预检结果。 */
@Data
@AllArgsConstructor
public class InviteCodeCheckDTO {
    /** 是否为一条可认领（INVITE_ISSUED 未认领）的有效邀请码。 */
    private boolean valid;
    /** 预生成时绑定的专业文本，供前端回显核对。 */
    private String major;
}
