package com.xju.sem.module.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xju.sem.module.profile.entity.AlumniPathCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * alumni_path_card 的 MyBatis-Plus Mapper。常规 CRUD/分页走 BaseMapper + LambdaQueryWrapper；
 * §6.3 的 GROUP BY 聚合与二级维度下钻用注解 SQL（在 DB 侧完成分组计数，不"读全表再内存分组"）。
 */
@Mapper
public interface AlumniPathCardMapper extends BaseMapper<AlumniPathCard> {

    /** §6.3 一级：按 destination_type 分组计数（仅 PUBLISHED、未删除）。 */
    @Select("SELECT destination_type AS bucketKey, COUNT(*) AS cnt " +
            "FROM alumni_path_card " +
            "WHERE deleted = 0 AND status = 'PUBLISHED' AND major_tag_id = #{majorTagId} " +
            "GROUP BY destination_type")
    List<DestinationCount> countByDestinationType(@Param("majorTagId") Long majorTagId);

    /** §6.3 二级下钻：EMPLOY 去向的行业分布（bucketKey=industry_tag_id）。 */
    @Select("SELECT CAST(industry_tag_id AS CHAR) AS bucketKey, COUNT(*) AS cnt " +
            "FROM alumni_path_card " +
            "WHERE deleted = 0 AND status = 'PUBLISHED' AND major_tag_id = #{majorTagId} " +
            "AND destination_type = 'EMPLOY' " +
            "GROUP BY industry_tag_id")
    List<DestinationCount> countEmployByIndustry(@Param("majorTagId") Long majorTagId);

    /** §6.3 二级下钻：POSTGRAD 去向的目标院校分布（bucketKey=target_school）。 */
    @Select("SELECT target_school AS bucketKey, COUNT(*) AS cnt " +
            "FROM alumni_path_card " +
            "WHERE deleted = 0 AND status = 'PUBLISHED' AND major_tag_id = #{majorTagId} " +
            "AND destination_type = 'POSTGRAD' " +
            "GROUP BY target_school")
    List<DestinationCount> countPostgradBySchool(@Param("majorTagId") Long majorTagId);

    /**
     * §6.4 冷启动放宽（C16）：本专业候选池为空时，按"同学院跨专业"收窄——JOIN alumni_profile.college
     * 过滤，而非退化为全平台放宽。仅 PUBLISHED、未删除，限量返回。
     */
    @Select("SELECT c.* FROM alumni_path_card c JOIN alumni_profile a ON c.user_id = a.user_id " +
            "WHERE c.deleted = 0 AND c.status = 'PUBLISHED' AND a.deleted = 0 AND a.college = #{college} " +
            "ORDER BY c.grad_year DESC, c.id DESC LIMIT #{limit}")
    List<AlumniPathCard> selectPublishedByCollege(@Param("college") String college,
                                                  @Param("limit") int limit);
}
