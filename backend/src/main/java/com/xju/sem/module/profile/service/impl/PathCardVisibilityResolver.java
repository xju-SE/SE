package com.xju.sem.module.profile.service.impl;

import com.xju.sem.module.profile.entity.AlumniPathCard;
import com.xju.sem.module.profile.enums.FieldGroup;
import com.xju.sem.module.profile.enums.Visibility;
import com.xju.sem.module.profile.dto.response.VisiblePathCardDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 字段级可见性计算引擎（§6.2 resolveVisibleFields）。<b>纯函数式</b>：只依赖入参（卡片 + 该卡的
 * 可见性配置 + 访问者与卡片的关系判定），不做任何 DB 访问，便于列表批量脱敏时复用与单测。
 *
 * <p>规则：本人/ADMIN 全量可见；否则对每个"该卡实际存在的字段分组"判定——PUBLIC 恒可见、
 * SAME_MAJOR 需 viewer 与卡片同专业、SELF 对他人恒不可见；缺省级别 SAME_MAJOR。
 * 不可见分组对应的列置 null，并记入 hiddenGroups 供前端渲染"该信息未公开"。
 *
 * <p>始终可见的上下文字段：{@code id/ownerUserId/status/majorTagId/destinationType}——majorTagId 与
 * destinationType 是卡片的分类维度（列表已按其过滤、统计已按其聚合，属低敏上下文），不参与逐组脱敏；
 * real_name 不落在路径卡表内，任何情况下都不会出现在本 DTO（对齐 real_name 永不 PUBLIC 红线）。
 */
@Component
public class PathCardVisibilityResolver {

    /**
     * @param card          路径卡
     * @param visByGroup    该卡的 fieldGroup→visibility 配置（缺省 SAME_MAJOR）
     * @param fullAccess    访问者是否本人或 ADMIN（全量可见）
     * @param sameMajor     访问者是否与卡片同专业（决定 SAME_MAJOR 级别是否放行）
     * @param ownerNickname 属主展示句柄（登录名，非真名）
     * @param ownerBadge    属主贡献者徽章
     */
    public VisiblePathCardDTO resolve(AlumniPathCard card,
                                      Map<String, String> visByGroup,
                                      boolean fullAccess,
                                      boolean sameMajor,
                                      String ownerNickname,
                                      Integer ownerBadge) {
        List<String> hidden = new ArrayList<>();
        VisiblePathCardDTO.VisiblePathCardDTOBuilder b = VisiblePathCardDTO.builder()
                .id(card.getId())
                .ownerUserId(card.getUserId())
                .ownerNickname(ownerNickname)
                .ownerContributorBadge(ownerBadge)
                .status(card.getStatus())
                // 始终可见的上下文维度
                .majorTagId(card.getMajorTagId())
                .destinationType(card.getDestinationType());

        for (FieldGroup group : FieldGroup.groupsFor(card.getDestinationType())) {
            boolean visible = groupVisible(group.name(), visByGroup, fullAccess, sameMajor);
            if (!visible) {
                hidden.add(group.name());
                continue;
            }
            switch (group) {
                case BASIC:
                    b.gradStage(card.getGradStage())
                            .gradYear(card.getGradYear())
                            .gradGpa(card.getGradGpa())
                            .gpaScale(card.getGpaScale());
                    break;
                case EMPLOY_LOCATION:
                    b.city(card.getCity()).industryTagId(card.getIndustryTagId());
                    break;
                case EMPLOY_DETAIL:
                    b.company(card.getCompany()).position(card.getPosition());
                    break;
                case POSTGRAD_TARGET:
                    b.postgradAdmissionType(card.getPostgradAdmissionType())
                            .targetSchool(card.getTargetSchool())
                            .targetMajor(card.getTargetMajor());
                    break;
                case POSTGRAD_SCORE:
                    b.examScore(card.getExamScore());
                    break;
                case POSTGRAD_INTERVIEW:
                    b.interviewExp(card.getInterviewExp());
                    break;
                case POSTGRAD_PREP:
                    b.prepMonths(card.getPrepMonths()).prepMaterials(card.getPrepMaterials());
                    break;
                case ADVICE:
                    b.advice(card.getAdvice());
                    break;
                default:
                    break;
            }
        }
        return b.hiddenGroups(hidden).build();
    }

    private boolean groupVisible(String group, Map<String, String> visByGroup,
                                 boolean fullAccess, boolean sameMajor) {
        if (fullAccess) {
            return true;
        }
        String vis = visByGroup.getOrDefault(group, Visibility.SAME_MAJOR.name());
        if (Visibility.PUBLIC.name().equals(vis)) {
            return true;
        }
        if (Visibility.SAME_MAJOR.name().equals(vis)) {
            return sameMajor;
        }
        // SELF 或未知值 → 对他人不可见
        return false;
    }
}
