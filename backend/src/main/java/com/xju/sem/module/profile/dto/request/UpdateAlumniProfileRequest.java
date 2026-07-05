package com.xju.sem.module.profile.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑毕业生本人档案入参（PUT /alumni-profiles/me）。学历/毕业年份/专业允许本人在档案层调整
 * （与具体某张路径卡的 grad_stage/major 独立）。徽章/计数为系统写入字段，不在本请求内。
 */
@Data
public class UpdateAlumniProfileRequest {

    @Size(max = 100, message = "学院名过长")
    private String college;

    /** 最高学历专业标签，须为 tag_type=MAJOR 的有效标签（Service 层校验）。 */
    private Long majorTagId;

    private Integer gradYear;

    /** BACHELOR/MASTER/PHD（Service 层校验）。 */
    private String degreeType;

    @Size(max = 500, message = "简介不超过500字")
    private String bio;

    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;
}
