package com.xju.sem.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 毕业生档案（表 alumni_profile），与 user 一对一。
 * 认证通过后由 M1 写入身份字段；helped/adopted 计数由 M4 采纳事件累加。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alumni_profile")
public class AlumniProfile extends BaseEntity {

    private Long userId;

    private String realName;

    private String college;

    /** 最高学历专业 FK→tag.id。 */
    private Long majorTagId;

    /** 最高学历毕业年份。 */
    private Integer gradYear;

    /** BACHELOR/MASTER/PHD。 */
    private String degreeType;

    /** 贡献者认证标识 0 无 1 有。 */
    private Integer isContributorBadge;

    /** 已帮助学弟学妹计数（缓存，M4 累加）。 */
    private Integer helpedCount;

    /** 被采纳次数（缓存）。 */
    private Integer adoptedCount;

    private String honorCertUrl;

    private String bio;

    private String avatarUrl;
}
