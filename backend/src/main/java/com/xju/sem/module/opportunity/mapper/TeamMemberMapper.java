package com.xju.sem.module.opportunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.opportunity.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** team_member 的 MyBatis-Plus Mapper。加入状态流转走 CAS，与 {@link TeamMapper} 同一风格。 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {

    /** 通用加入状态 CAS：仅当当前 join_status==from 时置为 to。 */
    @Update("UPDATE team_member SET join_status = #{to} WHERE id = #{id} AND join_status = #{from} AND deleted = 0")
    int casJoinStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to);
}
