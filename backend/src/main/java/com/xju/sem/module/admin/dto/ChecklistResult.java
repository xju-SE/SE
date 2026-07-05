package com.xju.sem.module.admin.dto;

import lombok.Data;

/**
 * "三秒可判断"隐私 checklist（仅 KNOWLEDGE_ENTRY 终审使用）。三项任一勾选，
 * {@link com.xju.sem.module.admin.service.AuditTaskService#decide} 会强制将决定转为 RETURN，
 * 忽略 ADMIN 实际传入的 decision（即使误选"通过"也拦截），见 07 详细设计 §6.3。
 */
@Data
public class ChecklistResult {

    /** 是否检出真实姓名。 */
    private boolean hasRealName;

    /** 是否检出联系方式（手机号/邮箱/微信QQ等）。 */
    private boolean hasContact;

    /** 是否检出可反向定位到个人的信息组合（如班级+姓名+宿舍号）。 */
    private boolean hasLocatableCombo;

    public boolean anyChecked() {
        return hasRealName || hasContact || hasLocatableCombo;
    }
}
