package com.xju.sem.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xju.sem.module.knowledge.entity.KnowledgeEntry;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * knowledge_entry 的 MyBatis-Plus Mapper。
 * FULLTEXT 全文搜索（§6.6）与原子计数/CAS 更新在 XML / 注解 SQL 中实现，不做"读出再写回"。
 */
public interface KnowledgeEntryMapper extends BaseMapper<KnowledgeEntry> {

    /**
     * FULLTEXT（ngram）全文搜索，附相关度 relevance，按相关度+浏览量+创建时间排序。
     * 第一个参数为 IPage，MyBatis-Plus 分页插件自动改写 COUNT + LIMIT/OFFSET。
     */
    IPage<KnowledgeEntry> fullTextSearch(IPage<KnowledgeEntry> page,
                                         @Param("keyword") String keyword,
                                         @Param("category") String category);

    /**
     * 短关键词（长度&lt;2）兜底：仅对 title 做 LIKE，不对 content 做 LIKE，避免全表扫描。
     */
    IPage<KnowledgeEntry> likeSearch(IPage<KnowledgeEntry> page,
                                     @Param("keyword") String keyword,
                                     @Param("category") String category);

    /** 浏览量原子 +1（SQL 级，非"读出再写回"；本期不做节流去重，见实现说明简化说明）。 */
    @Update("UPDATE knowledge_entry SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);

    /** 是否已存在某来源求助单生成过的候选（防重复生成，一个求助单只生成一条候选）。 */
    @Select("SELECT COUNT(*) FROM knowledge_entry WHERE deleted = 0 AND source_help_id = #{helpTicketId}")
    int countBySourceHelpId(@Param("helpTicketId") Long helpTicketId);

    /**
     * 过期自动降权定时任务（§6.7）扫描候选集：PUBLISHED 且 valid_until 已过期。
     * "降权"在缺少 weight_score 列的现有 schema 下，通过状态迁移到 EXPIRED 后退出
     * PUBLISHED-only 的列表/搜索默认可见范围来实现（详见实现说明）。
     */
    @Select("SELECT * FROM knowledge_entry WHERE deleted = 0 AND status = 'PUBLISHED' " +
            "AND valid_until IS NOT NULL AND valid_until < #{today}")
    List<KnowledgeEntry> selectExpirable(@Param("today") LocalDate today);

    /** 状态 CAS：仅当仍为 PUBLISHED 时才转 EXPIRED，防止与并发的手动下线/编辑重复处理。 */
    @Update("UPDATE knowledge_entry SET status = 'EXPIRED' WHERE id = #{id} AND status = 'PUBLISHED'")
    int expireIfPublished(@Param("id") Long id);
}
