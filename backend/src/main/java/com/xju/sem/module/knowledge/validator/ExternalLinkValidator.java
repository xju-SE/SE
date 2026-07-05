package com.xju.sem.module.knowledge.validator;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.knowledge.enums.KnowledgeCategory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * "高时效信息不自存"红线校验（§6.1），Service 层强制执行，不只依赖前端表单，
 * 防止越权 API 调用绕过治理规则：
 * - category=NAV：externalUrl 必填且必须是合法 http/https 链接；
 * - 其余类目：externalUrl 必须为空。
 *
 * <p>03 详细设计中同时要求的 externalSourceName（外链来源名称）字段在 schema.sql 的
 * knowledge_entry 表中未开列，本期不做持久化校验，仅保留 externalUrl 这条核心红线，
 * 见实现说明"假设与简化"一节。
 */
@Component
public class ExternalLinkValidator {

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);

    public void validate(String category, String externalUrl) {
        boolean isNav = KnowledgeCategory.NAV.name().equals(category);
        if (isNav) {
            if (!StringUtils.hasText(externalUrl) || !URL_PATTERN.matcher(externalUrl.trim()).matches()) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "公共信息导航类目必须填写合法的官方外链地址(http/https)");
            }
        } else {
            if (StringUtils.hasText(externalUrl)) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "非公共信息导航类目不可填写外链");
            }
        }
    }
}
