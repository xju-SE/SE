package com.xju.sem.module.help.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.help.entity.HelpAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * help_answer 的 MyBatis-Plus Mapper。
 * countAdopted 供 §6.2 路由打分复用（"该候选人历史同类问题/累计被采纳次数"），集中一处、避免 JOIN 逻辑散落。
 */
@Mapper
public interface HelpAnswerMapper extends BaseMapper<HelpAnswer> {

    /**
     * 统计某回答人被采纳的回答数（信任/专长打分维度）。
     * questionTypeTagId 非空则限定该问题类型；为 null 统计全部被采纳数。
     */
    @Select("SELECT COUNT(*) FROM help_answer a JOIN help_ticket t ON a.ticket_id = t.id " +
            "WHERE a.responder_id = #{responderId} AND a.is_adopted = 1 AND a.deleted = 0 AND t.deleted = 0 " +
            "AND (#{questionTypeTagId} IS NULL OR t.question_type_tag_id = #{questionTypeTagId})")
    int countAdopted(@Param("responderId") Long responderId,
                     @Param("questionTypeTagId") Long questionTypeTagId);

    /** 某求助单的回答条数（列表/详情 answerCount）。 */
    @Select("SELECT COUNT(*) FROM help_answer WHERE ticket_id = #{ticketId} AND deleted = 0")
    int countByTicket(@Param("ticketId") Long ticketId);
}
