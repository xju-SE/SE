package com.xju.sem.module.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 路径卡字段级可见性（表 path_visibility）。UK(path_card_id, field_group) 保证一卡一组一行。
 *
 * <p><b>注意</b>：schema 的 path_visibility 只有 id/path_card_id/field_group/visibility 四列，
 * <b>没有 deleted/created_at/updated_at</b>，故本实体不继承 {@link com.xju.sem.common.BaseEntity}
 * （若继承，其 @TableLogic deleted 会往 WHERE 注入不存在的列导致 SQL 失败）。
 */
@Data
@TableName("path_visibility")
public class PathVisibility implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属路径卡 FK→alumni_path_card.id。 */
    private Long pathCardId;

    /** 字段分组（见 {@code FieldGroup}）。 */
    private String fieldGroup;

    /** 可见级别 SELF/SAME_MAJOR/PUBLIC（见 {@code Visibility}），缺省 SAME_MAJOR。 */
    private String visibility;
}
