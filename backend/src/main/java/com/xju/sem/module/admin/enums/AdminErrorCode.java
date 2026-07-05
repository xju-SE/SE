package com.xju.sem.module.admin.enums;

/**
 * M7 剩余功能（标签/时间线维护/举报/贡献者认证/运营统计）在全局错误码分段内的具体值
 * （对齐 07 详细设计 §5，分段约定：2xxxx 参数校验、3xxxx 业务规则、4xxxx 资源不存在）。
 *
 * <p>放在模块内、以 {@code int} 常量 + {@code BusinessException(int, String)} 抛出，避免侵入
 * 地基 {@code ResultCode} 枚举，与 M6 {@code TimelineErrorCode} 同一处理方式。审核队列范围
 * （audit_task 相关）沿用既有代码已用的 {@code ResultCode} 通用常量，不在此重复登记。
 */
public final class AdminErrorCode {

    private AdminErrorCode() {
    }

    /** 参数校验：tagType 非法枚举值。 */
    public static final int TAG_TYPE_INVALID = 20701;
    /** 参数校验：report targetType/reasonType 非法枚举值。 */
    public static final int REPORT_PARAM_INVALID = 20702;
    /** 参数校验：timeline routeType/stage/字段越界。 */
    public static final int TIMELINE_PARAM_INVALID = 20703;

    /** 标签命名在同类型同父级下已存在（uk_type_name_parent）。 */
    public static final int TAG_NAME_DUPLICATE = 30704;
    /** 时间线模板已下线（OFFLINE）不可编辑节点。 */
    public static final int TIMELINE_TEMPLATE_OFFLINE = 30705;
    /** 时间线模板发布前置条件不满足（无节点）。 */
    public static final int TIMELINE_PUBLISH_PRECONDITION = 30706;
    /** 该举报目标已存在待处理记录（已合并说明至既有举报）。 */
    public static final int REPORT_DUPLICATE_PENDING = 30707;
    /** 举报当前状态不允许该操作（非 PENDING）。 */
    public static final int REPORT_STATE_CONFLICT = 30708;
    /** 贡献者认证申请人角色/认证状态不满足条件，或已有待处理申请。 */
    public static final int CONTRIBUTOR_CERT_NOT_ELIGIBLE = 30709;
    /** 该 targetType 的举报处置动作本期暂不支持自动执行（见举报处理"假设与简化"）。 */
    public static final int REPORT_ACTION_UNSUPPORTED = 30710;
    /** handleAction 与 targetType 不匹配（如对 USER 选择 CONTENT_HIDDEN）。 */
    public static final int REPORT_ACTION_MISMATCH = 30711;

    /** 审核任务不存在。 */
    public static final int AUDIT_TASK_NOT_FOUND = 40701;
    /** 举报不存在。 */
    public static final int REPORT_NOT_FOUND = 40702;
    /** 标签不存在。 */
    public static final int TAG_NOT_FOUND = 40703;
    /** 时间线模板/节点/引用不存在。 */
    public static final int TIMELINE_NOT_FOUND = 40704;
}
