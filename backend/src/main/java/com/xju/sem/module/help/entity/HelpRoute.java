package com.xju.sem.module.help.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 求助-校友路由匹配记录（对应 schema.sql {@code help_route} 表）★验收关键表。
 *
 * <p>该表结构精简（无 deleted/created_at/updated_at/batch_no/rank_no/route_type/match_reasons 列），
 * 故不继承 {@link com.xju.sem.common.BaseEntity}。唯一约束 uk_ticket_user(ticket_id, matched_user_id)
 * 保证同一人对同一单只产生一条路由记录、天然避免重复通知；"分批升级重试"通过按 ticket_id 查已匹配
 * 用户集合并在下一轮排除来实现，无需 batch_no 列（见 {@link com.xju.sem.module.help.service.impl.HelpRouteServiceImpl}）。
 */
@Data
@TableName("help_route")
public class HelpRoute implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属求助单 help_ticket.id。 */
    private Long ticketId;

    /** 被匹配到的候选人 user.id（校友或高年级学长）。 */
    private Long matchedUserId;

    /** 匹配总分（见 §6.2 打分公式），用于排序留痕与效果复盘。 */
    private Integer matchScore;

    /** NOTIFIED/VIEWED/ANSWERED/EXPIRED，见 {@link com.xju.sem.module.help.enums.HelpRouteStatus}。 */
    private String status;

    /** 通知发出时间（数据库有 DEFAULT，插入时应用层也显式赋值）。 */
    private LocalDateTime notifiedAt;
}
