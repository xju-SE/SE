package com.xju.sem.module.opportunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 队伍（表 team）。{@code opportunityId} 可空——schema 注释"关联机会(可空,自由组队)"，
 * 既支持挂靠某条机会发起组队，也支持不关联任何机会的自由组队。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("team")
public class Team extends BaseEntity {

    /** 所属机会，可空（自由组队）。 */
    private Long opportunityId;

    /** 队长（创建人）。 */
    private Long leaderId;

    private String title;

    private String description;

    /** 招募需求文本，如"前端1名、UI1名"。 */
    private String needDesc;

    /** 人数上限（含队长），2-20。 */
    private Integer capacity;

    /** 当前人数（含队长），冗余计数，创建时=1；并发名额竞争走 Mapper CAS，不做"读出再写回"。 */
    private Integer currentSize;

    /** RECRUITING/FULL/ONGOING/ENDED，见 {@code TeamStatus}。 */
    private String status;
}
