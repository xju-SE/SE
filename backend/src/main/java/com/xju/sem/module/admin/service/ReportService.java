package com.xju.sem.module.admin.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.admin.dto.HandleReportRequest;
import com.xju.sem.module.admin.dto.ReportDTO;
import com.xju.sem.module.admin.dto.ReportQuery;
import com.xju.sem.module.admin.dto.SubmitReportRequest;

/**
 * 举报受理（07 详细设计 §8）：FR-M7-09~12。目标类型分发按 {@code target_type} 转发调用对应
 * 模块已暴露的下架/下线/封禁方法，不重复实现内容治理动作本身。
 */
public interface ReportService {

    /** FR-M7-09 提交举报：同一 reporter 对同一 target 已有 PENDING 记录时合并说明而非新建。 */
    ReportDTO submit(Long reporterId, SubmitReportRequest request);

    /** FR-M7-10 举报队列（治理端），status 缺省按 PENDING。 */
    PageResult<ReportDTO> pageForAdmin(ReportQuery query);

    /** FR-M7-12 我提交的举报记录。 */
    PageResult<ReportDTO> pageMine(Long reporterId, String status, int page, int size);

    /** FR-M7-10 举报详情。 */
    ReportDTO getById(Long id);

    /** FR-M7-11 处理举报：UPHELD 时按 targetType 分发调用对应模块治理方法。 */
    ReportDTO handle(Long id, Long adminId, HandleReportRequest request);
}
