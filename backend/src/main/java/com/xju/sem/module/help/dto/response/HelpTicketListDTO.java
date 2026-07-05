package com.xju.sem.module.help.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 求助单列表响应（FR-M4-04）：分页数据 + 统计卡。对应 API 约定
 * {@code data:{records,total,page,size,statCard}}。
 */
@Data
public class HelpTicketListDTO {

    private List<HelpTicketDTO> records;
    private long total;
    private long page;
    private long size;

    /** 统计卡；仅在按具体专业过滤时计算，跨专业浏览时为 null。 */
    private TicketStatCardDTO statCard;
}
