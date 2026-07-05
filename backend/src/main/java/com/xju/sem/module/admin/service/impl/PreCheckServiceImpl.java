package com.xju.sem.module.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xju.sem.module.admin.dto.PreCheckResultDTO;
import com.xju.sem.module.admin.service.PreCheckService;
import com.xju.sem.module.knowledge.dto.response.KnowledgeEntryDTO;
import com.xju.sem.module.knowledge.enums.KnowledgeCategory;
import com.xju.sem.module.knowledge.service.KnowledgeEntryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * FR-M7-05 知识候选自动完整性/隐私预检（07 详细设计 §6.2）。正则扫描 title+content 识别
 * 手机号/邮箱/身份证号模式与"微信/QQ+数字"组合，并校验结构化字段完整性——结果仅作为人工审核
 * "三秒可判断" checklist 的预填提示，不替代人工判断，不允许系统据此自动退回。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreCheckServiceImpl implements PreCheckService {

    /** 中国大陆手机号（1开头，第二位3-9）。 */
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    /** 18 位身份证号（末位允许 X/x）。 */
    private static final Pattern ID_CARD = Pattern.compile("\\d{17}[0-9Xx]");
    /** "微信/加我/QQ/扣扣" 附近出现 5 位及以上数字，视为疑似联系方式（对应 §6.2 containsDigitsNear）。 */
    private static final Pattern CONTACT_KEYWORD_WITH_DIGITS =
            Pattern.compile("(微信|加我|QQ|扣扣)\\D{0,6}\\d{5,}");

    private final KnowledgeEntryService knowledgeEntryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PreCheckResultDTO runPreCheck(Long knowledgeEntryId) {
        // isAdmin=true 绕过"非 PUBLISHED 仅作者/认领人/ADMIN 可见"的可见性限制，取得完整正文用于扫描
        KnowledgeEntryDTO entry = knowledgeEntryService.getById(knowledgeEntryId, null, true);
        String text = safe(entry.getTitle()) + "\n" + safe(entry.getContent());

        boolean hasPhone = PHONE.matcher(text).find();
        boolean hasEmail = EMAIL.matcher(text).find();
        boolean hasIdCard = ID_CARD.matcher(text).find();
        boolean hasContactKeyword = CONTACT_KEYWORD_WITH_DIGITS.matcher(text).find();

        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(entry.getTitle())) {
            missing.add("title");
        }
        if (!StringUtils.hasText(entry.getContent())) {
            missing.add("content");
        }
        if (!KnowledgeCategory.isValid(entry.getCategory())) {
            missing.add("category");
        } else if (KnowledgeCategory.NAV.name().equals(entry.getCategory()) && !StringUtils.hasText(entry.getExternalUrl())) {
            // 完整性口径与 M3 §6.1 "公共信息导航强制外链" 红线一致，本处只做自动化前置提示，不重复定义规则本身
            missing.add("externalUrl");
        }

        return new PreCheckResultDTO(hasPhone || hasEmail || hasContactKeyword, hasIdCard, missing.isEmpty(), missing);
    }

    @Override
    public String serialize(PreCheckResultDTO result) {
        if (result == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            // audit_task.auto_precheck 为 VARCHAR(500)，超长兜底截断（正常结构化内容不会触达该长度）
            return json.length() > 500 ? json.substring(0, 500) : json;
        } catch (Exception e) {
            log.warn("预检结果序列化失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public PreCheckResultDTO deserialize(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PreCheckResultDTO.class);
        } catch (Exception e) {
            log.warn("预检结果解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
