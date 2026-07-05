package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 路径卡属主编辑视图（<b>不脱敏</b>）：POST/PUT /alumni-path-cards、以及本人查看自己卡片时返回。
 * 携带 version（供本人下次编辑做乐观锁）与完整字段级可见性配置。仅属主/ADMIN 可获得本视图。
 */
@Data
@Builder
public class AlumniPathCardDTO {

    private Long id;

    private Long userId;

    private String gradStage;
    private Long majorTagId;
    private Integer gradYear;
    private BigDecimal gradGpa;
    /** GPA 满分制 4/5（C11）。 */
    private Integer gpaScale;

    private String destinationType;

    private String city;
    private Long industryTagId;
    private String company;
    private String position;

    /** 深造录取方式 RECOMMEND(保研)/EXAM(考研)（S7）。 */
    private String postgradAdmissionType;
    private String targetSchool;
    private String targetMajor;
    private String examScore;
    private String interviewExp;
    private Integer prepMonths;
    /** 备考资料/参考书目（C10）。 */
    private String prepMaterials;

    private String advice;

    /** DRAFT/PUBLISHED/HIDDEN。 */
    private String status;

    /** 乐观锁版本号，编辑时须回传。 */
    private Integer version;

    /** 当前生效的字段级可见性配置（随去向类型重建）。 */
    private List<PathVisibilityDTO> visibilities;
}
