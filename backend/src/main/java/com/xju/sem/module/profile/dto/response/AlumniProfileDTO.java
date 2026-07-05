package com.xju.sem.module.profile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 毕业生本人档案出参（GET/PUT /alumni-profiles/me）。含贡献者徽章与帮助/采纳缓存计数。
 * real_name 仅在本人 /me 场景透出，跨模块摘要请用 {@link AlumniBriefDTO}。
 */
@Data
@Builder
public class AlumniProfileDTO {

    private Long userId;

    /** 强隐私，仅本人可见。 */
    private String realName;

    private String college;

    private Long majorTagId;

    private Integer gradYear;

    private String degreeType;

    private Integer isContributorBadge;

    private Integer helpedCount;

    private Integer adoptedCount;

    private String honorCertUrl;

    private String bio;

    private String avatarUrl;

    /** 成长标签（tag_type∈{INTEREST,GROWTH}）。 */
    private List<TagDTO> tags;
}
