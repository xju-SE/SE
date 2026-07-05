package com.xju.sem.module.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.profile.dto.request.PathCardRequest;
import com.xju.sem.module.profile.dto.request.UpdateVisibilityRequest;
import com.xju.sem.module.profile.dto.response.AlumniPathCardDTO;
import com.xju.sem.module.profile.dto.response.PathVisibilityDTO;
import com.xju.sem.module.profile.dto.response.VisiblePathCardDTO;
import com.xju.sem.module.profile.entity.AlumniPathCard;
import com.xju.sem.module.profile.entity.PathVisibility;
import com.xju.sem.module.profile.enums.DestinationType;
import com.xju.sem.module.profile.enums.FieldGroup;
import com.xju.sem.module.profile.enums.GradStage;
import com.xju.sem.module.profile.enums.PathCardStatus;
import com.xju.sem.module.profile.enums.PostgradAdmissionType;
import com.xju.sem.module.profile.enums.TagType;
import com.xju.sem.module.profile.enums.Visibility;
import com.xju.sem.module.profile.mapper.AlumniPathCardMapper;
import com.xju.sem.module.profile.mapper.PathVisibilityMapper;
import com.xju.sem.module.profile.service.AlumniPathCardService;
import com.xju.sem.module.user.dto.UserBriefDTO;
import com.xju.sem.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 校友路径卡服务实现。核心：§6.1 去向分支校验、§4 状态机、乐观锁（@Version）并发控制、
 * §6.2 字段级脱敏（委托 {@link PathCardVisibilityResolver}）与可见性分组重建。
 *
 * <p>错误码（本模块段）：20202 分支必填缺失；20203 field_group 非法；30201 未发布不可查看；
 * 30202 状态流转非法；30203 乐观锁冲突；40201 路径卡不存在。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlumniPathCardServiceImpl implements AlumniPathCardService {

    private static final int PREP_MONTHS_MAX = 60;
    /** GPA 满分制缺省值（C11）：请求未指定时按 4 分制。 */
    private static final int DEFAULT_GPA_SCALE = 4;

    private final AlumniPathCardMapper cardMapper;
    private final PathVisibilityMapper visibilityMapper;
    private final ProfileTagSupport tagSupport;
    private final IdentityProfileSupport identitySupport;
    private final PathCardVisibilityResolver visibilityResolver;
    private final UserService userService;

    // ==================== 本人 CRUD ====================

    @Override
    @Transactional
    public AlumniPathCardDTO create(Long userId, PathCardRequest request) {
        AlumniPathCard card = new AlumniPathCard();
        card.setUserId(userId);
        applyRequest(card, request);
        card.setStatus(PathCardStatus.DRAFT.name());
        card.setVersion(0);
        validateAndNormalize(card);
        // S6：同一校友每个毕业阶段至多一张路径卡（与 uk_user_stage 唯一键呼应，先行显式拦截给出友好错误）。
        requireStageNotDuplicated(userId, card.getGradStage());
        cardMapper.insert(card);
        rebuildVisibilityGroups(card.getId(), card.getDestinationType());
        return toOwnerDTO(card);
    }

    @Override
    @Transactional
    public AlumniPathCardDTO update(Long cardId, Long userId, boolean isAdmin, PathCardRequest request) {
        AlumniPathCard card = requireCard(cardId);
        requireOwnerOrAdmin(card, userId, isAdmin);
        applyRequest(card, request);
        if (request.getVersion() != null) {
            card.setVersion(request.getVersion());
        }
        validateAndNormalize(card);
        int rows = cardMapper.updateById(card);
        if (rows == 0) {
            throw new BusinessException(30203, "该路径卡已被更新，请刷新后重试");
        }
        rebuildVisibilityGroups(cardId, card.getDestinationType());
        return toOwnerDTO(requireCard(cardId));
    }

    @Override
    @Transactional
    public void delete(Long cardId, Long userId, boolean isAdmin) {
        AlumniPathCard card = requireCard(cardId);
        requireOwnerOrAdmin(card, userId, isAdmin);
        cardMapper.deleteById(cardId);
        // 可见性行无逻辑删除列，随卡物理清理
        visibilityMapper.delete(new LambdaQueryWrapper<PathVisibility>()
                .eq(PathVisibility::getPathCardId, cardId));
    }

    @Override
    public List<AlumniPathCardDTO> listMine(Long userId) {
        List<AlumniPathCard> cards = cardMapper.selectList(new LambdaQueryWrapper<AlumniPathCard>()
                .eq(AlumniPathCard::getUserId, userId)
                .orderByDesc(AlumniPathCard::getUpdatedAt));
        List<AlumniPathCardDTO> result = new ArrayList<>();
        for (AlumniPathCard c : cards) {
            result.add(toOwnerDTO(c));
        }
        return result;
    }

    // ==================== 状态机 ====================

    @Override
    @Transactional
    public String publish(Long cardId, Long userId) {
        return transit(cardId, userId, PathCardStatus.DRAFT, PathCardStatus.PUBLISHED);
    }

    @Override
    @Transactional
    public String withdraw(Long cardId, Long userId) {
        return transit(cardId, userId, PathCardStatus.PUBLISHED, PathCardStatus.DRAFT);
    }

    @Override
    @Transactional
    public String hidePathCardByReport(Long cardId, Long adminOperatorId, String reason) {
        AlumniPathCard card = requireCard(cardId);
        String cur = card.getStatus();
        if (!PathCardStatus.PUBLISHED.name().equals(cur) && !PathCardStatus.DRAFT.name().equals(cur)) {
            throw new BusinessException(30202, "仅 PUBLISHED/DRAFT 路径卡可下架，当前=" + cur);
        }
        card.setStatus(PathCardStatus.HIDDEN.name());
        updateWithLock(card);
        log.warn("路径卡下架 cardId={}, operator={}, reason={}", cardId, adminOperatorId, reason);
        return PathCardStatus.HIDDEN.name();
    }

    @Override
    @Transactional
    public String restorePathCard(Long cardId, Long adminOperatorId) {
        AlumniPathCard card = requireCard(cardId);
        if (!PathCardStatus.HIDDEN.name().equals(card.getStatus())) {
            throw new BusinessException(30202, "仅 HIDDEN 路径卡可复核恢复，当前=" + card.getStatus());
        }
        card.setStatus(PathCardStatus.PUBLISHED.name());
        updateWithLock(card);
        log.warn("路径卡复核恢复 cardId={}, operator={}", cardId, adminOperatorId);
        return PathCardStatus.PUBLISHED.name();
    }

    /** DRAFT/PUBLISHED 本人互转的通用流转（HIDDEN 不走本方法，见状态机约束）。 */
    private String transit(Long cardId, Long userId, PathCardStatus from, PathCardStatus to) {
        AlumniPathCard card = requireCard(cardId);
        requireOwnerOrAdmin(card, userId, false);
        if (!from.name().equals(card.getStatus())) {
            throw new BusinessException(30202,
                    "状态流转非法：需 " + from + "，当前=" + card.getStatus());
        }
        card.setStatus(to.name());
        updateWithLock(card);
        return to.name();
    }

    // ==================== 可见性配置 ====================

    @Override
    public List<PathVisibilityDTO> getVisibility(Long cardId, Long userId) {
        AlumniPathCard card = requireCard(cardId);
        requireOwnerOrAdmin(card, userId, false);
        Map<String, String> vis = loadVisMap(cardId);
        List<PathVisibilityDTO> result = new ArrayList<>();
        for (FieldGroup g : FieldGroup.groupsFor(card.getDestinationType())) {
            result.add(PathVisibilityDTO.builder()
                    .fieldGroup(g.name())
                    .visibility(vis.getOrDefault(g.name(), Visibility.SAME_MAJOR.name()))
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public List<PathVisibilityDTO> updateVisibility(Long cardId, Long userId, UpdateVisibilityRequest request) {
        AlumniPathCard card = requireCard(cardId);
        requireOwnerOrAdmin(card, userId, false);
        String dest = card.getDestinationType();
        for (UpdateVisibilityRequest.Item item : request.getItems()) {
            if (!FieldGroup.isAllowedFor(item.getFieldGroup(), dest)) {
                throw new BusinessException(20203,
                        "字段分组不属于当前去向类型: " + item.getFieldGroup());
            }
            if (!Visibility.isValid(item.getVisibility())) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "可见级别非法: " + item.getVisibility());
            }
            upsertVisibility(cardId, item.getFieldGroup(), item.getVisibility());
        }
        return getVisibility(cardId, userId);
    }

    // ==================== 浏览（脱敏）====================

    @Override
    public PageResult<VisiblePathCardDTO> pageList(Long majorTagId, String destinationType,
                                                   Integer gradYearFrom, Integer gradYearTo,
                                                   Long viewerId, boolean viewerIsAdmin,
                                                   int page, int size) {
        LambdaQueryWrapper<AlumniPathCard> w = new LambdaQueryWrapper<AlumniPathCard>()
                .eq(AlumniPathCard::getStatus, PathCardStatus.PUBLISHED.name());
        if (majorTagId != null) {
            w.eq(AlumniPathCard::getMajorTagId, majorTagId);
        }
        if (StringUtils.hasText(destinationType)) {
            w.eq(AlumniPathCard::getDestinationType, destinationType);
        }
        if (gradYearFrom != null) {
            w.ge(AlumniPathCard::getGradYear, gradYearFrom);
        }
        if (gradYearTo != null) {
            w.le(AlumniPathCard::getGradYear, gradYearTo);
        }
        w.orderByDesc(AlumniPathCard::getGradYear).orderByDesc(AlumniPathCard::getId);

        IPage<AlumniPathCard> p = cardMapper.selectPage(new Page<>(page, size), w);
        List<AlumniPathCard> cards = p.getRecords();

        Long viewerMajor = identitySupport.majorTagIdOf(viewerId);
        Map<Long, Map<String, String>> visMaps = loadVisMaps(collectIds(cards));
        Map<Long, UserBriefDTO> ownerBriefs = new HashMap<>();

        List<VisiblePathCardDTO> records = new ArrayList<>();
        for (AlumniPathCard c : cards) {
            records.add(desensitize(c, viewerId, viewerIsAdmin, viewerMajor,
                    visMaps.getOrDefault(c.getId(), new HashMap<>()), ownerBriefs));
        }
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public VisiblePathCardDTO getDetail(Long cardId, Long viewerId, boolean viewerIsAdmin) {
        AlumniPathCard card = requireCard(cardId);
        boolean fullAccess = viewerIsAdmin || (viewerId != null && viewerId.equals(card.getUserId()));
        if (!fullAccess && !PathCardStatus.PUBLISHED.name().equals(card.getStatus())) {
            throw new BusinessException(30201, "路径卡未发布，无权查看");
        }
        Long viewerMajor = identitySupport.majorTagIdOf(viewerId);
        return desensitize(card, viewerId, viewerIsAdmin, viewerMajor,
                loadVisMap(cardId), new HashMap<>());
    }

    // ==================== 跨模块契约 ====================

    @Override
    public VisiblePathCardDTO getVisiblePathCard(Long cardId, Long viewerId) {
        AlumniPathCard card = cardMapper.selectById(cardId);
        if (card == null) {
            return null;
        }
        boolean fullAccess = viewerId != null && viewerId.equals(card.getUserId());
        if (!fullAccess && !PathCardStatus.PUBLISHED.name().equals(card.getStatus())) {
            return null; // 契约方法宽容：不可展示时返回 null，交由 M6 决定占位
        }
        Long viewerMajor = identitySupport.majorTagIdOf(viewerId);
        return desensitize(card, viewerId, false, viewerMajor, loadVisMap(cardId), new HashMap<>());
    }

    @Override
    public boolean existsPathCard(Long id) {
        return id != null && cardMapper.selectById(id) != null;
    }

    @Override
    public boolean hasMajorTag(Long userId, Long majorTagId) {
        if (userId == null || majorTagId == null) {
            return false;
        }
        Long cnt = cardMapper.selectCount(new LambdaQueryWrapper<AlumniPathCard>()
                .eq(AlumniPathCard::getUserId, userId)
                .eq(AlumniPathCard::getMajorTagId, majorTagId));
        if (cnt != null && cnt > 0) {
            return true;
        }
        return identitySupport.alumniHasMajor(userId, majorTagId);
    }

    // ==================== §6.1 分支校验 ====================

    private void applyRequest(AlumniPathCard card, PathCardRequest r) {
        card.setGradStage(r.getGradStage());
        card.setMajorTagId(r.getMajorTagId());
        card.setGradYear(r.getGradYear());
        card.setGradGpa(r.getGradGpa());
        card.setGpaScale(r.getGpaScale());
        card.setDestinationType(r.getDestinationType());
        card.setCity(r.getCity());
        card.setIndustryTagId(r.getIndustryTagId());
        card.setCompany(r.getCompany());
        card.setPosition(r.getPosition());
        card.setPostgradAdmissionType(r.getPostgradAdmissionType());
        card.setTargetSchool(r.getTargetSchool());
        card.setTargetMajor(r.getTargetMajor());
        card.setExamScore(r.getExamScore());
        card.setInterviewExp(r.getInterviewExp());
        card.setPrepMonths(r.getPrepMonths());
        card.setPrepMaterials(r.getPrepMaterials());
        card.setAdvice(r.getAdvice());
    }

    /** §6.1：基础字段合法性 + 去向分支必填校验 + 清空非当前分支字段（避免脏数据残留）。 */
    private void validateAndNormalize(AlumniPathCard card) {
        if (!GradStage.isValid(card.getGradStage())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "毕业阶段非法");
        }
        if (!DestinationType.isValid(card.getDestinationType())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "去向类型非法");
        }
        tagSupport.requireType(card.getMajorTagId(), TagType.MAJOR.name());
        // C11：GPA 满分制取 4 或 5，缺省 4；grad_gpa 上界按本卡 gpa_scale 判定，不硬编码 5。
        if (card.getGpaScale() == null) {
            card.setGpaScale(DEFAULT_GPA_SCALE);
        }
        if (card.getGpaScale() != 4 && card.getGpaScale() != 5) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "GPA 满分制仅支持 4 或 5");
        }
        if (card.getGradGpa() != null
                && (card.getGradGpa().compareTo(BigDecimal.ZERO) < 0
                || card.getGradGpa().compareTo(BigDecimal.valueOf(card.getGpaScale())) > 0)) {
            throw new BusinessException(20201, "毕业 GPA 超出 [0," + card.getGpaScale() + "] 范围");
        }
        if (card.getPrepMonths() != null
                && (card.getPrepMonths() < 0 || card.getPrepMonths() > PREP_MONTHS_MAX)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "备考时长须在 [0,60] 月");
        }

        String dest = card.getDestinationType();
        if (DestinationType.isEmploy(dest)) {
            requireNotEmpty(card.getCity(), "工作城市");
            requireNotEmpty(card.getCompany(), "公司名称");
            requireNotEmpty(card.getPosition(), "岗位名称");
            if (card.getIndustryTagId() == null) {
                throw new BusinessException(20202, "就业去向缺少行业标签");
            }
            tagSupport.requireType(card.getIndustryTagId(), TagType.INDUSTRY.name());
            clearPostgradFields(card);
        } else if (DestinationType.isPostgrad(dest)) {
            requireNotEmpty(card.getTargetSchool(), "目标院校");
            requireNotEmpty(card.getTargetMajor(), "目标专业");
            // S7：深造须标注录取方式 RECOMMEND(保研)/EXAM(考研)；考研分支强制填写初试成绩构成。
            if (!PostgradAdmissionType.isValid(card.getPostgradAdmissionType())) {
                throw new BusinessException(20202, "深造去向须选择录取方式（保研/考研）");
            }
            if (PostgradAdmissionType.isExam(card.getPostgradAdmissionType())) {
                requireNotEmpty(card.getExamScore(), "初试成绩构成");
            }
            clearEmployFields(card);
        } else {
            clearEmployFields(card);
            clearPostgradFields(card);
        }
    }

    private void requireNotEmpty(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(20202, "该去向类型下 " + field + " 为必填");
        }
    }

    private void clearEmployFields(AlumniPathCard card) {
        card.setCity(null);
        card.setIndustryTagId(null);
        card.setCompany(null);
        card.setPosition(null);
    }

    private void clearPostgradFields(AlumniPathCard card) {
        card.setPostgradAdmissionType(null);
        card.setTargetSchool(null);
        card.setTargetMajor(null);
        card.setExamScore(null);
        card.setInterviewExp(null);
        card.setPrepMonths(null);
        card.setPrepMaterials(null);
    }

    // ==================== 可见性分组重建 / upsert ====================

    /** 按去向类型重建可见性分组：删除已不适用的分组行；缺失的分组按默认 SAME_MAJOR 补建（保留已有配置）。 */
    private void rebuildVisibilityGroups(Long cardId, String destinationType) {
        List<FieldGroup> applicable = FieldGroup.groupsFor(destinationType);
        Set<String> applicableNames = new HashSet<>();
        for (FieldGroup g : applicable) {
            applicableNames.add(g.name());
        }
        List<PathVisibility> existing = visibilityMapper.selectList(
                new LambdaQueryWrapper<PathVisibility>().eq(PathVisibility::getPathCardId, cardId));
        Set<String> existingGroups = new HashSet<>();
        for (PathVisibility v : existing) {
            if (!applicableNames.contains(v.getFieldGroup())) {
                visibilityMapper.deleteById(v.getId());
            } else {
                existingGroups.add(v.getFieldGroup());
            }
        }
        for (String g : applicableNames) {
            if (!existingGroups.contains(g)) {
                PathVisibility v = new PathVisibility();
                v.setPathCardId(cardId);
                v.setFieldGroup(g);
                v.setVisibility(Visibility.SAME_MAJOR.name());
                visibilityMapper.insert(v);
            }
        }
    }

    private void upsertVisibility(Long cardId, String fieldGroup, String visibility) {
        PathVisibility exist = visibilityMapper.selectOne(new LambdaQueryWrapper<PathVisibility>()
                .eq(PathVisibility::getPathCardId, cardId)
                .eq(PathVisibility::getFieldGroup, fieldGroup));
        if (exist == null) {
            PathVisibility v = new PathVisibility();
            v.setPathCardId(cardId);
            v.setFieldGroup(fieldGroup);
            v.setVisibility(visibility);
            visibilityMapper.insert(v);
        } else {
            exist.setVisibility(visibility);
            visibilityMapper.updateById(exist);
        }
    }

    private Map<String, String> loadVisMap(Long cardId) {
        Map<String, String> map = new HashMap<>();
        for (PathVisibility v : visibilityMapper.selectList(new LambdaQueryWrapper<PathVisibility>()
                .eq(PathVisibility::getPathCardId, cardId))) {
            map.put(v.getFieldGroup(), v.getVisibility());
        }
        return map;
    }

    private Map<Long, Map<String, String>> loadVisMaps(List<Long> cardIds) {
        Map<Long, Map<String, String>> maps = new HashMap<>();
        if (cardIds.isEmpty()) {
            return maps;
        }
        List<PathVisibility> rows = visibilityMapper.selectList(new LambdaQueryWrapper<PathVisibility>()
                .in(PathVisibility::getPathCardId, cardIds));
        for (PathVisibility v : rows) {
            maps.computeIfAbsent(v.getPathCardId(), k -> new HashMap<>())
                    .put(v.getFieldGroup(), v.getVisibility());
        }
        return maps;
    }

    // ==================== 脱敏组装 ====================

    private VisiblePathCardDTO desensitize(AlumniPathCard card, Long viewerId, boolean viewerIsAdmin,
                                           Long viewerMajor, Map<String, String> visMap,
                                           Map<Long, UserBriefDTO> ownerBriefCache) {
        boolean fullAccess = viewerIsAdmin || (viewerId != null && viewerId.equals(card.getUserId()));
        boolean sameMajor = viewerMajor != null && viewerMajor.equals(card.getMajorTagId());
        UserBriefDTO ub = ownerBriefCache.computeIfAbsent(card.getUserId(), userService::getBrief);
        String nickname = ub == null ? null : ub.getUsername();
        Integer badge = ownerBadge(card.getUserId());
        return visibilityResolver.resolve(card, visMap, fullAccess, sameMajor, nickname, badge);
    }

    private Integer ownerBadge(Long ownerUserId) {
        var ap = identitySupport.findAlumniProfile(ownerUserId);
        return ap == null ? null : ap.getIsContributorBadge();
    }

    // ==================== 通用辅助 ====================

    /** S6：同一 (user_id, grad_stage) 已有路径卡则拒绝新建（DUPLICATE 30003）。 */
    private void requireStageNotDuplicated(Long userId, String gradStage) {
        Long cnt = cardMapper.selectCount(new LambdaQueryWrapper<AlumniPathCard>()
                .eq(AlumniPathCard::getUserId, userId)
                .eq(AlumniPathCard::getGradStage, gradStage));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ResultCode.DUPLICATE,
                    "该毕业阶段已存在路径卡，请直接编辑：" + gradStage);
        }
    }

    private AlumniPathCard requireCard(Long cardId) {
        AlumniPathCard card = cardMapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException(40201, "路径卡不存在或已删除");
        }
        return card;
    }

    private void requireOwnerOrAdmin(AlumniPathCard card, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (userId == null || !userId.equals(card.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该路径卡");
        }
    }

    private void updateWithLock(AlumniPathCard card) {
        int rows = cardMapper.updateById(card);
        if (rows == 0) {
            throw new BusinessException(30203, "该路径卡已被更新，请刷新后重试");
        }
    }

    private List<Long> collectIds(List<AlumniPathCard> cards) {
        List<Long> ids = new ArrayList<>();
        for (AlumniPathCard c : cards) {
            ids.add(c.getId());
        }
        return ids;
    }

    private AlumniPathCardDTO toOwnerDTO(AlumniPathCard card) {
        List<PathVisibilityDTO> vis = new ArrayList<>();
        Map<String, String> map = loadVisMap(card.getId());
        for (FieldGroup g : FieldGroup.groupsFor(card.getDestinationType())) {
            vis.add(PathVisibilityDTO.builder()
                    .fieldGroup(g.name())
                    .visibility(map.getOrDefault(g.name(), Visibility.SAME_MAJOR.name()))
                    .build());
        }
        return AlumniPathCardDTO.builder()
                .id(card.getId())
                .userId(card.getUserId())
                .gradStage(card.getGradStage())
                .majorTagId(card.getMajorTagId())
                .gradYear(card.getGradYear())
                .gradGpa(card.getGradGpa())
                .gpaScale(card.getGpaScale())
                .destinationType(card.getDestinationType())
                .city(card.getCity())
                .industryTagId(card.getIndustryTagId())
                .company(card.getCompany())
                .position(card.getPosition())
                .postgradAdmissionType(card.getPostgradAdmissionType())
                .targetSchool(card.getTargetSchool())
                .targetMajor(card.getTargetMajor())
                .examScore(card.getExamScore())
                .interviewExp(card.getInterviewExp())
                .prepMonths(card.getPrepMonths())
                .prepMaterials(card.getPrepMaterials())
                .advice(card.getAdvice())
                .status(card.getStatus())
                .version(card.getVersion())
                .visibilities(vis)
                .build();
    }
}
