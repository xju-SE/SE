package com.xju.sem.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.admin.entity.Tag;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * tag 表的 MyBatis-Plus Mapper（物理归属本模块，见 {@link Tag} 类注释）。
 *
 * <p>{@link #countUsage} 是一次<b>受控的跨表只读聚合</b>：直接 COUNT 各模块中持有 {@code tag_id}
 * 外键的列，不通过 Service 契约——07 详细设计 §6.7 的停用引用检查算法本身就要求"聚合各标签被
 * user_tag/help_ticket 等引用的次数（只读聚合，不改写来源表）"，与 M1 {@code MajorTagMapper} 只读
 * 反查 {@code tag} 表同一"受控例外"性质（纯只读、不写、不越权），而非常规的"跨模块查表"违规。
 * 新增业务表若引入新的 tag_id 外键，只需在本 SQL 追加一个 UNION 分支。
 */
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 统计某标签被多少处业务记录引用：user_tag.tag_id / student_profile.major_tag_id·
     * target_industry_tag_id / alumni_profile.major_tag_id / alumni_path_card.major_tag_id·
     * industry_tag_id / help_ticket.major_tag_id·question_type_tag_id / timeline_template.major_tag_id。
     * 供 §6.7 停用二次确认提示"该标签仍被 N 处引用"。
     */
    @Select("SELECT "
            + "  (SELECT COUNT(*) FROM user_tag WHERE tag_id = #{tagId}) "
            + "+ (SELECT COUNT(*) FROM student_profile WHERE deleted = 0 AND (major_tag_id = #{tagId} OR target_industry_tag_id = #{tagId})) "
            + "+ (SELECT COUNT(*) FROM alumni_profile WHERE deleted = 0 AND major_tag_id = #{tagId}) "
            + "+ (SELECT COUNT(*) FROM alumni_path_card WHERE deleted = 0 AND (major_tag_id = #{tagId} OR industry_tag_id = #{tagId})) "
            + "+ (SELECT COUNT(*) FROM help_ticket WHERE deleted = 0 AND (major_tag_id = #{tagId} OR question_type_tag_id = #{tagId})) "
            + "+ (SELECT COUNT(*) FROM timeline_template WHERE deleted = 0 AND major_tag_id = #{tagId}) "
            + "AS cnt")
    long countUsage(@Param("tagId") Long tagId);
}
