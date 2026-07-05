package com.xju.sem.module.help.service;

import com.xju.sem.module.help.dto.response.HelpRouteDTO;

import java.util.List;

/**
 * 求助-校友路由匹配服务（★系统灵魂核心算法 §6.2）。
 */
public interface HelpRouteService {

    /**
     * 对求助单执行一轮路由匹配：构建候选池（逐级放宽）→ 标签打分 → 排序取 TopK →
     * 写 help_route 并逐条发 HELP_MATCH 通知；本轮命中≥1人则 OPEN→MATCHED。
     *
     * @param ticketId              求助单 id
     * @param excludeUserIds        本轮需额外排除的候选（重试时传上一批已通知者，避免重复打扰）
     */
    void routeHelpTicket(Long ticketId, List<Long> excludeUserIds);

    /** 查看某求助单的路由匹配记录（诊断/复盘，仅求助人/ADMIN）。 */
    List<HelpRouteDTO> listRoutes(Long ticketId, Long viewerId);
}
