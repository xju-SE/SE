package com.xju.sem.module.help.service;

import com.xju.sem.module.help.dto.request.CreateHelpTicketRequest;
import com.xju.sem.module.help.dto.request.HelpTicketQuery;
import com.xju.sem.module.help.dto.response.HelpTicketDTO;
import com.xju.sem.module.help.dto.response.HelpTicketDetailDTO;
import com.xju.sem.module.help.dto.response.HelpTicketListDTO;
import com.xju.sem.module.help.dto.response.TicketStatCardDTO;

/**
 * 求助单服务：发布（含档案快照）、列表（本专业高频仪表盘）、详情聚合、撤回、关闭。
 * 状态机流转与乐观（CAS）并发控制在实现类内完成。
 */
public interface HelpTicketService {

    /** FR-M4-01 发布求助单：快照专业/年级，落 OPEN，提交后异步触发路由匹配。 */
    HelpTicketDTO createTicket(Long askerId, CreateHelpTicketRequest request);

    /** FR-M4-05 详情：求助单 + 回答（含追问线程）+ 当前用户操作位。 */
    HelpTicketDetailDTO getDetail(Long ticketId, Long viewerId);

    /** FR-M4-04 列表：本专业高频，统计卡 + 分页表格。 */
    HelpTicketListDTO listTickets(HelpTicketQuery query, Long viewerId);

    /** FR-M4-13 撤回求助单：仅 OPEN/MATCHED 且无回答时允许，软删除。 */
    void withdraw(Long ticketId, Long operatorId);

    /** FR-M4-12 关闭求助单：任意非终态 → CLOSED（closeReason 本期仅日志留痕）。 */
    void close(Long ticketId, Long operatorId, String closeReason);

    /**
     * 供 M7 治理端只读展示来源摘要（04 §8：审核知识候选时回溯原始求助单）。
     * 本期复用 {@link HelpTicketDTO} 作为"简要 DTO"，不再另建 HelpTicketSummaryDTO。
     */
    HelpTicketDTO getSummary(Long ticketId);

    /**
     * 供 M7 举报处理调用：隐藏/软处置求助单（对齐 M2
     * {@code AlumniPathCardService#hidePathCardByReport} 既有模式，schema 无独立 HIDDEN 状态列，
     * 复用 {@code deleted} 逻辑删除位——隐藏后自动从列表/详情/统计查询中排除）。
     */
    void hideTicket(Long ticketId, Long adminOperatorId, String reason);

    /** 供 M7 调用：复核恢复被 {@link #hideTicket} 隐藏的求助单（deleted 1→0）。 */
    void restoreTicket(Long ticketId, Long adminOperatorId);

    /**
     * 供首页仪表盘调用：本人学院 OPEN 求助单统计卡。help_ticket 无 college 列，本期以本人
     * majorTagId 作为学院粒度的代理范围（与 {@link #listTickets} 统计卡口径一致），如需真正按
     * college 聚合需 M1 补充跨用户 college 查询契约。
     */
    TicketStatCardDTO getMyCollegeOpenTicketStats(Long userId);
}
