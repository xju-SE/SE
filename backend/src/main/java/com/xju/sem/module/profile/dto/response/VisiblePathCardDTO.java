package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 按访问者脱敏后的路径卡视图（跨模块契约）：{@code AlumniPathCardService.getVisiblePathCard(cardId, viewerId)}
 * 的返回类型，供 M6 时间线节点引用只读展示；同时复用为 GET /alumni-path-cards（列表行）与
 * GET /alumni-path-cards/{id}（详情）的响应体。
 *
 * <p>脱敏语义（§6.2）：对访问者不可见的字段分组，其对应列一律置为 null，并在 {@code hiddenGroups}
 * 中列出被隐藏的分组名，供前端渲染"该信息未公开"占位。ownerNickname 为登录名，绝不含 real_name。
 */
@Data
@Builder
public class VisiblePathCardDTO {

    private Long id;

    private Long ownerUserId;

    /** 属主展示句柄（登录名），非真实姓名。 */
    private String ownerNickname;

    /** 属主是否贡献者（信任标识，透传给展示层）。 */
    private Integer ownerContributorBadge;

    /** DRAFT/PUBLISHED/HIDDEN。 */
    private String status;

    // ---- BASIC 分组 ----
    private String gradStage;
    private Long majorTagId;
    private Integer gradYear;
    private BigDecimal gradGpa;
    /** GPA 满分制 4/5（C11，随 gradGpa 同组展示以便正确解读）。 */
    private Integer gpaScale;

    private String destinationType;

    // ---- EMPLOY_LOCATION ----
    private String city;
    private Long industryTagId;
    // ---- EMPLOY_DETAIL ----
    private String company;
    private String position;

    // ---- POSTGRAD_TARGET ----
    /** 深造录取方式 RECOMMEND(保研)/EXAM(考研)（S7）。 */
    private String postgradAdmissionType;
    private String targetSchool;
    private String targetMajor;
    // ---- POSTGRAD_SCORE ----
    private String examScore;
    // ---- POSTGRAD_INTERVIEW ----
    private String interviewExp;
    // ---- POSTGRAD_PREP ----
    private Integer prepMonths;
    /** 备考资料/参考书目（C10）。 */
    private String prepMaterials;

    // ---- ADVICE ----
    private String advice;

    /** 对当前访问者被隐藏的字段分组名列表（前端据此渲染占位）。 */
    private List<String> hiddenGroups;
}
