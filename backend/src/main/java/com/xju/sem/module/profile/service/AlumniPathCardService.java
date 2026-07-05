package com.xju.sem.module.profile.service;

import com.xju.sem.common.result.PageResult;
import com.xju.sem.module.profile.dto.request.PathCardRequest;
import com.xju.sem.module.profile.dto.request.UpdateVisibilityRequest;
import com.xju.sem.module.profile.dto.response.AlumniPathCardDTO;
import com.xju.sem.module.profile.dto.response.PathVisibilityDTO;
import com.xju.sem.module.profile.dto.response.VisiblePathCardDTO;

import java.util.List;

/**
 * 校友路径卡服务：CRUD、状态机（§4）、字段级可见性（§6.2）、乐观锁并发控制（§9）。
 * 含<b>跨模块契约方法</b>：getVisiblePathCard（供 M6 节点引用脱敏展示）、existsPathCard、
 * hasMajorTag（供 M4/M6 判断专业归属）、hidePathCardByReport/restorePathCard（供 M7 治理）。
 */
public interface AlumniPathCardService {

    // ---------------- 本人 CRUD ----------------

    /** FR-M2-03 新建路径卡（默认 DRAFT），按 §6.1 分支校验并重建可见性分组。 */
    AlumniPathCardDTO create(Long userId, PathCardRequest request);

    /** FR-M2-03 编辑路径卡（乐观锁 version，§6.1 分支校验，重建可见性分组）。 */
    AlumniPathCardDTO update(Long cardId, Long userId, boolean isAdmin, PathCardRequest request);

    /** FR-M2 软删除路径卡（本人或 ADMIN）。 */
    void delete(Long cardId, Long userId, boolean isAdmin);

    /** 本人路径卡列表（P05，不脱敏，含 version 与可见性配置）。 */
    List<AlumniPathCardDTO> listMine(Long userId);

    // ---------------- 状态机 ----------------

    /** FR-M2-04 发布：DRAFT → PUBLISHED（仅本人）。返回新状态。 */
    String publish(Long cardId, Long userId);

    /** FR-M2-04 撤回：PUBLISHED → DRAFT（仅本人）。返回新状态。 */
    String withdraw(Long cardId, Long userId);

    /** FR-M2-11 举报下架：PUBLISHED/DRAFT → HIDDEN（供 M7 调用）。返回新状态。 */
    String hidePathCardByReport(Long cardId, Long adminOperatorId, String reason);

    /** FR-M2-11 复核恢复：HIDDEN → PUBLISHED（供 M7 调用）。返回新状态。 */
    String restorePathCard(Long cardId, Long adminOperatorId);

    // ---------------- 可见性配置 ----------------

    /** FR-M2-05 获取字段级可见性配置（仅本人）。 */
    List<PathVisibilityDTO> getVisibility(Long cardId, Long userId);

    /** FR-M2-05 更新字段级可见性配置（仅本人，fieldGroup 须属当前去向合法分组）。 */
    List<PathVisibilityDTO> updateVisibility(Long cardId, Long userId, UpdateVisibilityRequest request);

    // ---------------- 浏览（按访问者脱敏）----------------

    /** FR-M2-06 分页浏览已发布路径卡，按访问者身份逐卡脱敏（§6.2）。 */
    PageResult<VisiblePathCardDTO> pageList(Long majorTagId, String destinationType,
                                            Integer gradYearFrom, Integer gradYearTo,
                                            Long viewerId, boolean viewerIsAdmin,
                                            int page, int size);

    /** FR-M2-07 详情：非本人/非 ADMIN 且非 PUBLISHED 抛 30201；否则按 §6.2 逐分组脱敏。 */
    VisiblePathCardDTO getDetail(Long cardId, Long viewerId, boolean viewerIsAdmin);

    // ---------------- 跨模块契约 ----------------

    /** 跨模块契约（供 M6 节点引用）：按访问者脱敏的路径卡；不存在或对访问者不可展示时返回 null（不抛异常）。 */
    VisiblePathCardDTO getVisiblePathCard(Long cardId, Long viewerId);

    /** 跨模块契约：路径卡是否存在且未删除。 */
    boolean existsPathCard(Long id);

    /** 跨模块契约（供 M4/M6）：该用户是否拥有指定专业标签（任一路径卡或其毕业档案命中）。 */
    boolean hasMajorTag(Long userId, Long majorTagId);
}
