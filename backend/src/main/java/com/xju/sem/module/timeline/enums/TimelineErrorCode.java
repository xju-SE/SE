package com.xju.sem.module.timeline.enums;

/**
 * M6 成长时间线在全局错误码分段内的具体值（对齐 06 详细设计 §5"错误码"表）。
 *
 * <p>放在模块内、以 {@code int} 常量 + {@code BusinessException(int, String)} 抛出，避免侵入
 * 地基 {@code ResultCode} 枚举（分段约定：2xxxx 参数校验、3xxxx 业务规则、4xxxx 资源不存在）。
 */
public final class TimelineErrorCode {

    private TimelineErrorCode() {
    }

    /** 参数校验：routeType 非合法枚举。 */
    public static final int ROUTE_TYPE_INVALID = 20601;
    /** 参数校验：stage 非法 / suggestedMonth 越界 / importance 越界 / orderNo 为负。 */
    public static final int NODE_PARAM_INVALID = 20602;
    /** 业务规则：该 major×route_type 尚未配置 PUBLISHED 模板（且无通用兜底）。 */
    public static final int TEMPLATE_NOT_CONFIGURED = 30601;
    /** 业务规则：模板/节点当前非 PUBLISHED，非 ADMIN 不可查看。 */
    public static final int NOT_PUBLISHED = 30602;
    /** 业务规则：UNDECIDED 不可经"选择/切换路线"主动选入。 */
    public static final int UNDECIDED_NOT_SELECTABLE = 30603;
    /** 业务规则：同 route_type 下通用模板 / 同 major×route 模板已存在（补 DB 无唯一索引之缺）。 */
    public static final int DUPLICATE_TEMPLATE = 30604;

    // ---- 以下为治理端维护（07 详细设计 §6.8，M7 侧新增，与上方读侧一同复用本类，避免重复建错误码类） ----

    /** 治理端维护：模板已下线（OFFLINE）不可编辑节点/引用（07 详细设计 §6.8）。 */
    public static final int TEMPLATE_OFFLINE_CANNOT_EDIT = 30605;
    /** 治理端维护：模板发布前置条件不满足（无节点，07 详细设计 §6.8）。 */
    public static final int PUBLISH_NO_NODE = 30606;

    /** 资源不存在：模板/节点/引用不存在或已删除，或引用的 refId 在对应模块不存在。 */
    public static final int RESOURCE_NOT_FOUND = 40601;
}
