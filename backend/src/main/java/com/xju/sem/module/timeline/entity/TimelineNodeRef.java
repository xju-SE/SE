package com.xju.sem.module.timeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 节点关联引用（表 timeline_node_ref，只存 ID，绝不复制被引用对象正文）。
 *
 * <p><b>不继承 {@link com.xju.sem.common.BaseEntity}</b>：schema.sql 该表仅 {@code (id, node_id,
 * ref_type, ref_id)} 四列，无 {@code deleted/created_at/updated_at}——故引用维护走"物理删旧插新"
 * 的覆盖式重建，不做逻辑删除。跨表引用不建 DB 级 FK，存在性由应用层按 {@code refType} 分别调用
 * 对应模块 Service 校验（与 M4 notification.ref_id 同一处理方式）。
 */
@Data
@TableName("timeline_node_ref")
public class TimelineNodeRef implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属节点 id。 */
    private Long nodeId;

    /** ALUMNI_PATH_CARD/KNOWLEDGE_ENTRY/OPPORTUNITY，见 {@code RefType}。 */
    private String refType;

    /** 被引用对象主键（按 refType 分别对应各模块表 id）。 */
    private Long refId;
}
