package com.xju.sem.module.user.dto;

import lombok.Data;

/** M7 审核列表分页查询条件。 */
@Data
public class AuthApplicationQuery {
    private String status;
    private String applyRole;
    private String verifyMethod;
    private long page = 1;
    private long size = 10;
}
