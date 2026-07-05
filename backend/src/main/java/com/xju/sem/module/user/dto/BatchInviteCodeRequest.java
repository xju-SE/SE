package com.xju.sem.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ADMIN 批量生成毕业生邀请码入参。
 * <p>说明：schema 的 auth_application 无 invite_expire_at / grad_year 列，故有效期与届别不落库；
 * major 以 major_text 快照存入预生成记录，认领终审时解析为 major_tag_id。
 */
@Data
public class BatchInviteCodeRequest {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 500, message = "单次最多生成500个")
    private Integer count;

    /** 专业文本（写入预生成记录的 major_text）。 */
    private String major;

    /** 学院文本（可选）。 */
    private String college;
}
