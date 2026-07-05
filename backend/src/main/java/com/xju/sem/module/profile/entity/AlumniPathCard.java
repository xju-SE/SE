package com.xju.sem.module.profile.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 校友路径卡（表 alumni_path_card）。字段与列名严格对齐 schema.sql（去向分支列在不同
 * destination_type 下是否必填由 Service 层校验，DB 层不设 NOT NULL）。
 *
 * <p>并发：本人编辑与管理员下架/复核可能并发写入同一行，故用 {@link Version} 乐观锁
 * （{@code MybatisPlusConfig} 已注册 OptimisticLockerInnerInterceptor）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alumni_path_card")
public class AlumniPathCard extends BaseEntity {

    /** 所属校友 user.id。 */
    private Long userId;

    /** 毕业阶段/学历 BACHELOR/MASTER/PHD（见 {@code GradStage}）。 */
    private String gradStage;

    /** 该阶段毕业专业 FK→tag.id (tag_type=MAJOR)。 */
    private Long majorTagId;

    /** 该阶段毕业年份。 */
    private Integer gradYear;

    /**
     * 毕业时 GPA（供推荐做 GPA 相近度匹配）。
     * <p>可空字段统一用 {@code updateStrategy=IGNORED}：编辑为"全量覆盖"语义，切换去向类型时
     * §6.1 会把非当前分支字段清空为 null，须让 updateById 真正写入 null（默认 NOT_NULL 策略会跳过 null，
     * 导致脏数据残留）。必填字段（gradStage/majorTagId/gradYear/destinationType/status）保持默认策略。
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal gradGpa;

    /**
     * GPA 满分制 4 或 5（C11）：grad_gpa 的上界校验按本列取值判定，禁止硬编码 5。
     * 必填（DB NOT NULL DEFAULT 4），Service 层缺省补 4，保持默认写入策略。
     */
    private Integer gpaScale;

    /** 去向类型 EMPLOY/POSTGRAD/CIVIL_SERVICE/ABROAD/...（见 {@code DestinationType}）。 */
    private String destinationType;

    // ---------------- 就业分支（destination_type=EMPLOY 必填）----------------
    /** 工作城市。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String city;
    /** 行业标签 FK→tag.id (tag_type=INDUSTRY)。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long industryTagId;
    /** 公司名称。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String company;
    /** 岗位名称。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String position;

    // ---------------- 深造分支（destination_type=POSTGRAD 必填目标院校/专业）----------------
    /** 深造录取方式 RECOMMEND(保研)/EXAM(考研)，POSTGRAD 必填（见 {@code PostgradAdmissionType}）。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String postgradAdmissionType;
    /** 目标院校。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String targetSchool;
    /** 目标专业。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String targetMajor;
    /** 初试成绩构成（自由文本，如 "政治68/英语75/数学132/专业课128/总分403"）。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String examScore;
    /** 复试经历（形式、机试/小论文、时间线）。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String interviewExp;
    /** 备考时长（月），范围 [0,60]。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Integer prepMonths;
    /** 备考资料/参考书目（自由文本，C10）。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String prepMaterials;

    // ---------------- 通用 ----------------
    /** 一句话经验总结 + 给学弟学妹的建议。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String advice;

    /** 状态 DRAFT/PUBLISHED/HIDDEN（见 {@code PathCardStatus}）。 */
    private String status;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
