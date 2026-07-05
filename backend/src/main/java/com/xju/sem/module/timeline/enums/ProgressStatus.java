package com.xju.sem.module.timeline.enums;

/**
 * 个人节点进度态（对齐 schema.sql user_progress.status 注释，见 §4.2）。双向可切换，无终态。
 *
 * <pre>
 *   [*] ──首次建立进度/懒初始化/路线切换──▶ NOT_STARTED
 *   NOT_STARTED ──标记完成，写 marked_at──▶ DONE
 *   DONE ──取消标记，清空 marked_at──▶ NOT_STARTED
 * </pre>
 */
public enum ProgressStatus {

    NOT_STARTED,
    DONE;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (ProgressStatus s : values()) {
            if (s.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
