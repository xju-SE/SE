package com.xju.sem.module.help.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.help.entity.HelpRoute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * help_route 的 MyBatis-Plus Mapper，并承载 §6.2 候选池构建的跨表只读查询。
 *
 * <p><b>跨表只读授权说明</b>：下列 select*Candidate/By* 方法直连他模块物理表
 * user / student_profile / alumni_profile 做 JOIN 取候选池，是本期为打通"系统灵魂"闭环的
 * 临时实现（任务书已明确授权）。未来由 M2 暴露 {@code listVerifiedUsersByMajor} 服务接口后，
 * 应删除这些 SQL 改走 Service 契约，本模块不再触碰他模块表。
 */
@Mapper
public interface HelpRouteMapper extends BaseMapper<HelpRoute> {

    // ---------------- 候选池构建（跨表只读，未来迁 M2） ----------------

    /** 同专业、已认证、启用中的校友。 */
    @Select("SELECT u.id AS user_id, u.role AS role, ap.major_tag_id AS major_tag_id, NULL AS grade_level " +
            "FROM user u JOIN alumni_profile ap ON ap.user_id = u.id " +
            "WHERE u.deleted = 0 AND u.role = 'ALUMNI' AND u.auth_status = 'VERIFIED' AND u.status = 'ACTIVE' " +
            "AND ap.deleted = 0 AND ap.major_tag_id = #{majorTagId}")
    List<CandidateRow> selectVerifiedAlumniByMajor(@Param("majorTagId") Long majorTagId);

    /** 同专业、已认证、启用中、且年级高于求助人的高年级学长/学姐。 */
    @Select("SELECT u.id AS user_id, u.role AS role, sp.major_tag_id AS major_tag_id, sp.grade_level AS grade_level " +
            "FROM user u JOIN student_profile sp ON sp.user_id = u.id " +
            "WHERE u.deleted = 0 AND u.role = 'STUDENT' AND u.auth_status = 'VERIFIED' AND u.status = 'ACTIVE' " +
            "AND sp.deleted = 0 AND sp.major_tag_id = #{majorTagId} AND sp.grade_level > #{minGradeLevel}")
    List<CandidateRow> selectVerifiedSeniorStudentsByMajor(@Param("majorTagId") Long majorTagId,
                                                           @Param("minGradeLevel") Integer minGradeLevel);

    /** 全平台已认证校友（同专业不足时的跨专业兜底，见 §6.2 第一步逐级放宽）。 */
    @Select("SELECT u.id AS user_id, u.role AS role, ap.major_tag_id AS major_tag_id, NULL AS grade_level " +
            "FROM user u JOIN alumni_profile ap ON ap.user_id = u.id " +
            "WHERE u.deleted = 0 AND u.role = 'ALUMNI' AND u.auth_status = 'VERIFIED' AND u.status = 'ACTIVE' " +
            "AND ap.deleted = 0 LIMIT 200")
    List<CandidateRow> selectAllVerifiedAlumni();

    /** 管理员兜底（极端冷启动无任何校友时，保证"≥1 次匹配通知"验收标准恒成立）。 */
    @Select("SELECT u.id AS user_id, u.role AS role, NULL AS major_tag_id, NULL AS grade_level " +
            "FROM user u WHERE u.deleted = 0 AND u.role = 'ADMIN' AND u.status = 'ACTIVE' LIMIT 1")
    List<CandidateRow> selectAnyAdmin();

    // ---------------- 路由记录读写 ----------------

    /** 该求助单已匹配过的候选人 id（重试时排除，避免重复通知同一人）。 */
    @Select("SELECT matched_user_id FROM help_route WHERE ticket_id = #{ticketId}")
    List<Long> listMatchedUserIds(@Param("ticketId") Long ticketId);

    /** 回答提交时回写：该候选人已对本单响应（响应率复盘）。 */
    @Update("UPDATE help_route SET status = 'ANSWERED' " +
            "WHERE ticket_id = #{ticketId} AND matched_user_id = #{userId} AND status <> 'ANSWERED'")
    int markResponded(@Param("ticketId") Long ticketId, @Param("userId") Long userId);
}
