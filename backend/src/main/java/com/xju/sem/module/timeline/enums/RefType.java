package com.xju.sem.module.timeline.enums;

/**
 * 节点关联引用类型（对齐 schema.sql timeline_node_ref.ref_type 注释）。
 *
 * <p>本模块只存 {@code (ref_type, ref_id)}，绝不复制被引用对象正文；展示与存在性校验时按本枚举
 * 路由到对应模块的只读 Service（M2 {@code AlumniPathCardService}、M3 {@code KnowledgeEntryService}、
 * M5 {@code OpportunityService}），实现"零复制、现取"（§9）。
 */
public enum RefType {

    /** 引用 M2 校友路径卡。 */
    ALUMNI_PATH_CARD,
    /** 引用 M3 知识条目。 */
    KNOWLEDGE_ENTRY,
    /** 引用 M5 机会。 */
    OPPORTUNITY;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (RefType t : values()) {
            if (t.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
