package com.xju.sem.module.profile.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-标签关联（表 user_tag）。UK(user_id, tag_id) 保证一人一标签一条。
 *
 * <p><b>注意</b>：schema 的 user_tag 只有 id/user_id/tag_id/tag_source/created_at 五列，
 * <b>没有 deleted/updated_at</b>，故本实体不继承 {@link com.xju.sem.common.BaseEntity}，
 * 覆盖式更新采用"物理删除旧行 + 插入新行"（见 UserTagServiceImpl），不做软删。
 */
@Data
@TableName("user_tag")
public class UserTag implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long tagId;

    /** 标签来源 SELF/SYSTEM（见 {@code TagSource}），本模块自选一律 SELF。 */
    private String tagSource;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
