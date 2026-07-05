package com.xju.sem.module.admin.service;

import com.xju.sem.module.admin.dto.PreCheckResultDTO;

/**
 * 知识候选自动完整性/隐私预检（FR-M7-05，07 详细设计 §6.2）。独立接口便于单元测试
 * （纯函数式输入输出，无副作用外的 DB 写；落库由 {@link AuditTaskService} 负责）。
 */
public interface PreCheckService {

    /** 对指定知识条目做正则扫描 + 结构化字段完整性校验，仅返回结果，不落库。 */
    PreCheckResultDTO runPreCheck(Long knowledgeEntryId);

    /** 序列化为 JSON 字符串（超长截断，适配 audit_task.auto_precheck VARCHAR(500)）。 */
    String serialize(PreCheckResultDTO result);

    /** 反序列化；输入为空或解析失败返回 null。 */
    PreCheckResultDTO deserialize(String json);
}
