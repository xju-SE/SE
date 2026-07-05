package com.xju.sem.module.admin.enums;

/**
 * report.status 状态机（对齐 schema.sql 列注释：PENDING/HANDLED/DISMISSED）。三态单步判定，
 * 不设 PROCESSING 中间态——处理是 ADMIN 一次性原子决定（同 audit_task 的 CAS 终审模式），见
 * 07 详细设计 §4.2。{@code HANDLED} 对应 07 文档"UPHELD"语义（举报成立并已按 handle_note 中记录
 * 的动作码处置），{@code DISMISSED} 对应"举报不成立"。
 */
public enum ReportStatus {
    PENDING,
    HANDLED,
    DISMISSED
}
