package com.xju.sem.module.opportunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.opportunity.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * team 的 MyBatis-Plus Mapper。名额竞争（并发审批）用 {@link #incrementIfBelowCapacity} 做
 * "WHERE current_size &lt; capacity"的 CAS 防超员，与状态 CAS 同一原则，不做"读出再写回"。
 */
@Mapper
public interface TeamMapper extends BaseMapper<Team> {

    /** 通用状态 CAS：仅当当前状态==from 时置为 to。 */
    @Update("UPDATE team SET status = #{to} WHERE id = #{id} AND status = #{from} AND deleted = 0")
    int casStatus(@Param("id") Long id, @Param("from") String from, @Param("to") String to);

    /** 审批通过时原子 +1，且仅当未满员才生效（§6.5 并发抢最后一个名额的 CAS 保护）。 */
    @Update("UPDATE team SET current_size = current_size + 1 " +
            "WHERE id = #{id} AND current_size < capacity AND deleted = 0")
    int incrementIfBelowCapacity(@Param("id") Long id);

    /** 成员退出/被移除时原子 -1（下限 0 保护，正常情况下队长恒在，不会真正触底）。 */
    @Update("UPDATE team SET current_size = current_size - 1 " +
            "WHERE id = #{id} AND current_size > 0 AND deleted = 0")
    int decrementIfAboveZero(@Param("id") Long id);
}
