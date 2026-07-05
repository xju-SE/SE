package com.xju.sem.module.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新建/编辑校友路径卡入参（POST /alumni-path-cards、PUT /alumni-path-cards/{id}，FR-M2-03）。
 * 去向分支字段的必填性随 {@code destinationType} 变化，由 Service 层按 §6.1 分支校验（20202）；
 * 非当前分支的字段在保存时被统一清空，避免脏数据残留。
 */
@Data
public class PathCardRequest {

    /** BACHELOR/MASTER/PHD。 */
    @NotBlank(message = "毕业阶段必选")
    private String gradStage;

    @NotNull(message = "专业必选")
    private Long majorTagId;

    @NotNull(message = "毕业年份必填")
    private Integer gradYear;

    private BigDecimal gradGpa;

    /** GPA 满分制 4 或 5（C11，Service 层校验取值并据此判 grad_gpa 上界）；缺省按 4。 */
    private Integer gpaScale;

    /** EMPLOY/POSTGRAD/CIVIL_SERVICE/ABROAD/ENTREPRENEUR/FLEXIBLE/OTHER。 */
    @NotBlank(message = "去向类型必选")
    private String destinationType;

    // ---- EMPLOY 分支 ----
    @Size(max = 50)
    private String city;
    private Long industryTagId;
    @Size(max = 100)
    private String company;
    @Size(max = 100)
    private String position;

    // ---- POSTGRAD 分支 ----
    /** RECOMMEND(保研)/EXAM(考研)，POSTGRAD 必填、EXAM 时 examScore 必填（Service 层校验）。 */
    @Size(max = 12)
    private String postgradAdmissionType;
    @Size(max = 100)
    private String targetSchool;
    @Size(max = 100)
    private String targetMajor;
    @Size(max = 100)
    private String examScore;
    @Size(max = 1000)
    private String interviewExp;
    /** 备考时长（月），范围 [0,60]（Service 层校验）。 */
    private Integer prepMonths;
    /** 备考资料/参考书目（C10）。 */
    @Size(max = 1000)
    private String prepMaterials;

    // ---- 通用 ----
    @Size(max = 2000)
    private String advice;

    /** 编辑时的乐观锁版本号；新建时可空。 */
    private Integer version;
}
