package com.xju.sem.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.knowledge.entity.KnowledgeFeedback;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** knowledge_feedback 的 MyBatis-Plus Mapper。 */
public interface KnowledgeFeedbackMapper extends BaseMapper<KnowledgeFeedback> {

    /**
     * 按类型分组计数（替代 03 详细设计中冗余的 knowledge_entry.useful_count 等计数列——
     * 现有 schema 未开该列，改为对本表的实时聚合查询，见实现说明）。
     */
    @Select("SELECT feedback_type AS feedbackType, COUNT(*) AS cnt FROM knowledge_feedback " +
            "WHERE deleted = 0 AND entry_id = #{entryId} GROUP BY feedback_type")
    List<FeedbackTypeCount> countByType(@Param("entryId") Long entryId);
}
