package com.xju.sem.module.help.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.help.entity.HelpFollowup;
import org.apache.ibatis.annotations.Mapper;

/** help_followup 的 MyBatis-Plus Mapper。基础 CRUD 由 BaseMapper 提供，按 ticket_id 查线程用条件构造器。 */
@Mapper
public interface HelpFollowupMapper extends BaseMapper<HelpFollowup> {
}
