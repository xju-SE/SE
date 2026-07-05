package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 字段级可见性配置行（GET/PUT /alumni-path-cards/{id}/visibility 的元素）。
 */
@Data
@Builder
public class PathVisibilityDTO {

    /** 字段分组（见 {@code FieldGroup}）。 */
    private String fieldGroup;

    /** 可见级别 SELF/SAME_MAJOR/PUBLIC。 */
    private String visibility;
}
