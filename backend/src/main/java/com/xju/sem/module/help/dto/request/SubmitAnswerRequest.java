package com.xju.sem.module.help.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交/编辑三段式回答入参（FR-M4-06 / FR-M4-07）。
 * precondition、cautions 可空；steps 为有序步骤数组，至少一步。
 */
@Data
public class SubmitAnswerRequest {

    @Size(max = 500, message = "适用前提不能超过500字")
    private String precondition;

    @NotEmpty(message = "操作步骤不能为空")
    private List<String> steps;

    @Size(max = 500, message = "注意事项不能超过500字")
    private String cautions;
}
