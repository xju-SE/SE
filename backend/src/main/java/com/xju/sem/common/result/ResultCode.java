package com.xju.sem.common.result;

import lombok.Getter;

/**
 * 全局错误码。分段约定（对齐地基 §4）：
 * 1xxxx 认证/权限；2xxxx 参数校验；3xxxx 业务规则；4xxxx 资源不存在；5xxxx 服务器错误。
 * 各模块业务错误在对应段内自行追加常量即可（如 30401 求助单已关闭）。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success"),

    // 1xxxx 认证 / 权限
    UNAUTHORIZED(10001, "未登录或登录已过期"),
    TOKEN_INVALID(10002, "无效的令牌"),
    FORBIDDEN(10003, "无权限执行该操作"),
    NOT_VERIFIED(10004, "身份未认证，无法执行写操作"),
    ACCOUNT_DISABLED(10005, "账号已被禁用"),
    BAD_CREDENTIALS(10006, "用户名或密码错误"),

    // 2xxxx 参数校验
    PARAM_INVALID(20001, "参数校验失败"),
    PARAM_MISSING(20002, "缺少必要参数"),

    // 3xxxx 业务规则
    BIZ_ERROR(30000, "业务处理失败"),
    STATE_CONFLICT(30001, "当前状态不允许该操作"),
    OPTIMISTIC_LOCK(30002, "数据已被他人修改，请刷新后重试"),
    DUPLICATE(30003, "记录已存在"),
    LIMIT_EXCEEDED(30004, "超出次数上限"),

    // 4xxxx 资源不存在
    NOT_FOUND(40001, "资源不存在"),

    // 5xxxx 服务器
    SERVER_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
