package com.xju.sem.module.timeline.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.timeline.dto.request.CreateTimelineTemplateRequest;
import com.xju.sem.module.timeline.dto.request.TimelineTemplateQuery;
import com.xju.sem.module.timeline.dto.request.UpdateTimelineTemplateRequest;
import com.xju.sem.module.timeline.dto.response.MajorTimelineStatsDTO;
import com.xju.sem.module.timeline.dto.response.TimelineTemplateDTO;

/**
 * 时间线模板服务（ADMIN 维护 + §6.2 解析 + 运营统计）。模板由 ADMIN 直接维护并自助发布，
 * 不经 M7 审核队列（§1"明确不做"）。发布态走状态 CAS，NULL 通用模板查重在应用层兜底（§6.2）。
 */
public interface TimelineTemplateService {

    /** FR-M6-01 新建模板（默认 DRAFT），按 §6.2 查重后创建。 */
    TimelineTemplateDTO create(Long adminId, CreateTimelineTemplateRequest request);

    /** FR-M6-01 编辑模板基本信息（名称）。 */
    TimelineTemplateDTO update(Long id, Long adminId, UpdateTimelineTemplateRequest request);

    /** FR-M6-02 发布：DRAFT/OFFLINE → PUBLISHED（状态 CAS）。 */
    TimelineTemplateDTO publish(Long id, Long adminId);

    /** FR-M6-02 下线：PUBLISHED → OFFLINE（状态 CAS）。 */
    TimelineTemplateDTO offline(Long id, Long adminId, String reason);

    /** 软删除模板（任意状态）。 */
    void delete(Long id, Long adminId);

    /** FR-M6-01 附带列表（按 major/route/status 过滤）。 */
    PageResult<TimelineTemplateDTO> page(TimelineTemplateQuery query);

    /** 模板详情（ADMIN 可见任意状态）。 */
    TimelineTemplateDTO getById(Long id);

    /**
     * §6.2 解析：major × routeType → 具体已发布模板（专业专属优先，缺失回退全专业通用）。
     * 无任何可用模板抛 {@code TEMPLATE_NOT_CONFIGURED(30601)}。跨内部服务契约（供 UserProgressService）。
     */
    TimelineTemplateDTO resolve(Long majorTagId, String routeType);

    /** FR-M6-12 专业级完成度统计（供 M7/首页仪表盘运营视角）。 */
    MajorTimelineStatsDTO getMajorTimelineStats(Long majorTagId, String routeType);
}
