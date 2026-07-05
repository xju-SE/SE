package com.xju.sem.module.knowledge.enums;

/**
 * 知识条目分类。取值与 schema.sql {@code knowledge_entry.category} 列注释严格一致
 * （LIFE/COURSE/COMPETITION/POSTGRAD_EMPLOY/NAV），entity 字段仍以 String 存储，
 * 本枚举仅供 Service 层校验/分支使用，不作为 MyBatis 类型处理器绑定字段类型。
 */
public enum KnowledgeCategory {
    /** 生活 */
    LIFE,
    /** 课程 */
    COURSE,
    /** 竞赛 */
    COMPETITION,
    /** 考研就业 */
    POSTGRAD_EMPLOY,
    /** 公共信息导航（高时效信息只可外链，不自存正文数值，见 §6.1 红线） */
    NAV;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (KnowledgeCategory c : values()) {
            if (c.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
