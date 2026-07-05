package com.xju.sem.module.user.constant;

/**
 * 角色枚举，供 {@code UserService.getRole(Long)} 对外返回（M4/M5 判定发布权限用）。
 * 数据库以字符串存储，此处仅为跨模块调用方便的强类型视图，无持久化 GUEST。
 */
public enum Role {
    STUDENT,
    ALUMNI,
    ADMIN;

    public static Role of(String name) {
        return Role.valueOf(name);
    }
}
