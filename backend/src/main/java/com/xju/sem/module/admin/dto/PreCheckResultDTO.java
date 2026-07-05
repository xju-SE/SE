package com.xju.sem.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识候选自动完整性/隐私预检结果（07 详细设计 §6.2）。序列化为 JSON 字符串落在
 * {@code audit_task.auto_precheck}（VARCHAR(500)），供审核详情页预填 checklist 提示——
 * 仅供人工判断参考，不替代人工判断，不允许系统依据本结果自动退回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreCheckResultDTO {

    /** 正则/关键词命中手机号、邮箱或"微信/QQ+数字"组合。 */
    private boolean contactSignal;

    /** 正则命中 18 位身份证号模式。 */
    private boolean idNumberSignal;

    /** 结构化字段是否完整（标题/正文/分类，NAV 分类还需 externalUrl）。 */
    private boolean fieldsComplete;

    /** fieldsComplete=false 时列出缺失字段名，供详情页提示。 */
    private List<String> missingFields;
}
