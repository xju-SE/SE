package com.xju.sem.module.profile.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 更新字段级可见性配置入参（PUT /alumni-path-cards/{id}/visibility，FR-M2-05）。
 * 每项的 fieldGroup 须属于该卡当前 destination_type 的合法分组（否则 20203），
 * visibility∈{SELF,SAME_MAJOR,PUBLIC}。
 */
@Data
public class UpdateVisibilityRequest {

    @NotEmpty(message = "可见性配置不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        private String fieldGroup;
        private String visibility;
    }
}
