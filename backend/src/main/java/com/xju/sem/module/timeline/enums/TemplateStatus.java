package com.xju.sem.module.timeline.enums;

/**
 * 时间线模板发布态（对齐 schema.sql timeline_template.status 注释，见 §4.1）。
 *
 * <pre>
 *   [*] ──ADMIN创建──▶ DRAFT
 *   DRAFT/OFFLINE ──发布──▶ PUBLISHED
 *   PUBLISHED ──下线──▶ OFFLINE
 *   任意态 ──软删除──▶ [*]
 * </pre>
 *
 * <p>仅 {@link #PUBLISHED} 参与 §6.2 模板解析并对学生可见；{@code OFFLINE}（内容仍在、暂不展示）
 * 与 {@code deleted=1}（记录永久移除）语义严格区分，所有解析查询须显式带 {@code status='PUBLISHED'
 * AND deleted=0}。
 */
public enum TemplateStatus {

    DRAFT,
    PUBLISHED,
    OFFLINE;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (TemplateStatus s : values()) {
            if (s.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
