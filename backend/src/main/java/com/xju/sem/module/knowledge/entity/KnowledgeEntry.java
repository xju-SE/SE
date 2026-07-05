package com.xju.sem.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 知识条目（对应 schema.sql {@code knowledge_entry} 表，精确列以该表为准）。
 * 枚举列（category/status/source_type）按地基约定用 String 存储，不引入 MyBatis 枚举 TypeHandler，
 * 对应的 Java 枚举定义见 {@code module.knowledge.enums} 包，仅供 Service 层校验/分支使用。
 *
 * <p>与 03 详细设计文档的差异（以 schema.sql 为准的精简，详见实现说明文档"假设与简化"一节）：
 * 不持有 external_source_name / claimed_at / reviewer_id / review_comment / published_at /
 * last_reviewed_at / weight_score / useful_count / outdated_count / need_update_count 等冗余列；
 * 三态评价计数改为对 {@link KnowledgeFeedback} 的实时聚合查询，审核意见/时间已由 M7 audit_task
 * 表（reviewer_id/decision_note/decided_at）承载，不在本表重复存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_entry")
public class KnowledgeEntry extends BaseEntity {

    /** 标题。 */
    private String title;

    /** 正文（Markdown 纯文本）；category=NAV 时仅存导航说明，不承载具体时效数值。 */
    private String content;

    /** LIFE/COURSE/COMPETITION/POSTGRAD_EMPLOY/NAV。 */
    private String category;

    /** 原始贡献者（原创作者，或求助被采纳时的回答人）。 */
    private Long authorId;

    /** 适用范围描述文本；NULL 表示通用不限。 */
    private String applicableScope;

    /** 有效期至；NULL 表示长期有效经验。 */
    private LocalDate validUntil;

    /** 官方外链地址；category=NAV 时必填，其余类目必须为空。 */
    private String externalUrl;

    /** CANDIDATE/REVIEWING/PUBLISHED/EXPIRED/OFFLINE。 */
    private String status;

    /** ORIGINAL/FROM_HELP。 */
    private String sourceType;

    /** source_type=FROM_HELP 时必填，只存来源求助单 ID，不复制内容。 */
    private Long sourceHelpId;

    /** 当前认领更新人；NULL 表示无人认领（默认由 author 维护）。 */
    private Long claimerId;

    /** 浏览量。 */
    private Integer viewCount;

    /** 乐观锁（内容编辑与状态流转共用同一把锁，见实现说明"并发控制"一节）。 */
    @Version
    private Integer version;

    /** 全文搜索相关度（仅 search 结果携带，非持久化列）。 */
    @TableField(exist = false)
    private Double relevance;
}
