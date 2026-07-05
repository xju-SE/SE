package com.xju.sem.module.timeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.timeline.dto.request.CreateTimelineTemplateRequest;
import com.xju.sem.module.timeline.dto.request.TimelineTemplateQuery;
import com.xju.sem.module.timeline.dto.request.UpdateTimelineTemplateRequest;
import com.xju.sem.module.timeline.dto.response.MajorTimelineStatsDTO;
import com.xju.sem.module.timeline.dto.response.TimelineTemplateDTO;
import com.xju.sem.module.timeline.entity.TimelineNode;
import com.xju.sem.module.timeline.entity.TimelineTemplate;
import com.xju.sem.module.timeline.entity.UserProgress;
import com.xju.sem.module.timeline.enums.ProgressStatus;
import com.xju.sem.module.timeline.enums.RouteType;
import com.xju.sem.module.timeline.enums.TemplateStatus;
import com.xju.sem.module.timeline.enums.TimelineErrorCode;
import com.xju.sem.module.timeline.mapper.TimelineNodeMapper;
import com.xju.sem.module.timeline.mapper.TimelineTemplateMapper;
import com.xju.sem.module.timeline.mapper.UserProgressMapper;
import com.xju.sem.module.timeline.service.TimelineTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间线模板服务实现：CRUD、§4.1 发布状态机（状态 CAS）、§6.2 解析（专业专属优先 + 全专业通用
 * 兜底）、NULL/非 NULL 双场景查重（schema 该表无唯一索引，全在应用层兜底）、FR-M6-12 专业级统计。
 */
@Service
@RequiredArgsConstructor
public class TimelineTemplateServiceImpl implements TimelineTemplateService {

    private final TimelineTemplateMapper templateMapper;
    private final TimelineNodeMapper nodeMapper;
    private final UserProgressMapper userProgressMapper;

    @Override
    public TimelineTemplateDTO create(Long adminId, CreateTimelineTemplateRequest request) {
        if (!RouteType.isValid(request.getRouteType())) {
            throw new BusinessException(TimelineErrorCode.ROUTE_TYPE_INVALID, "路线类型取值不合法");
        }
        // §6.2 查重：NULL 通用模板与非 NULL 专业模板均在应用层兜底（DB 无唯一索引）
        if (request.getMajorTagId() == null) {
            if (templateMapper.countGeneric(request.getRouteType()) > 0) {
                throw new BusinessException(TimelineErrorCode.DUPLICATE_TEMPLATE, "该路线已存在全专业通用模板");
            }
        } else if (templateMapper.countByMajorRoute(request.getMajorTagId(), request.getRouteType()) > 0) {
            throw new BusinessException(TimelineErrorCode.DUPLICATE_TEMPLATE, "该专业该路线已存在模板");
        }
        TimelineTemplate t = new TimelineTemplate();
        t.setMajorTagId(request.getMajorTagId());
        t.setRouteType(request.getRouteType());
        t.setName(request.getName());
        t.setStatus(TemplateStatus.DRAFT.name());
        t.setCreatedBy(adminId);
        templateMapper.insert(t);
        return TimelineMapping.toTemplateDTO(t);
    }

    @Override
    public TimelineTemplateDTO update(Long id, Long adminId, UpdateTimelineTemplateRequest request) {
        TimelineTemplate t = requireTemplate(id);
        t.setName(request.getName());
        templateMapper.updateById(t);
        return TimelineMapping.toTemplateDTO(t);
    }

    @Override
    public TimelineTemplateDTO publish(Long id, Long adminId) {
        TimelineTemplate t = requireTemplate(id);
        String cur = t.getStatus();
        if (!TemplateStatus.DRAFT.name().equals(cur) && !TemplateStatus.OFFLINE.name().equals(cur)) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅草稿或已下线的模板可发布");
        }
        if (templateMapper.casStatus(id, cur, TemplateStatus.PUBLISHED.name()) == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "模板状态已变更，请刷新后重试");
        }
        t.setStatus(TemplateStatus.PUBLISHED.name());
        return TimelineMapping.toTemplateDTO(t);
    }

    @Override
    public TimelineTemplateDTO offline(Long id, Long adminId, String reason) {
        TimelineTemplate t = requireTemplate(id);
        if (!TemplateStatus.PUBLISHED.name().equals(t.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅已发布的模板可下线");
        }
        if (templateMapper.casStatus(id, TemplateStatus.PUBLISHED.name(), TemplateStatus.OFFLINE.name()) == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "模板状态已变更，请刷新后重试");
        }
        t.setStatus(TemplateStatus.OFFLINE.name());
        return TimelineMapping.toTemplateDTO(t);
    }

    @Override
    public void delete(Long id, Long adminId) {
        requireTemplate(id);
        templateMapper.deleteById(id);
    }

    @Override
    public PageResult<TimelineTemplateDTO> page(TimelineTemplateQuery query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 100);
        QueryWrapper<TimelineTemplate> wrapper = new QueryWrapper<>();
        if (query.getMajorTagId() != null) {
            wrapper.eq("major_tag_id", query.getMajorTagId());
        }
        if (StringUtils.hasText(query.getRouteType())) {
            wrapper.eq("route_type", query.getRouteType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq("status", query.getStatus());
        }
        wrapper.orderByDesc("id");
        Page<TimelineTemplate> p = templateMapper.selectPage(new Page<>(page, size), wrapper);
        List<TimelineTemplateDTO> records = p.getRecords().stream().map(TimelineMapping::toTemplateDTO).toList();
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public TimelineTemplateDTO getById(Long id) {
        return TimelineMapping.toTemplateDTO(requireTemplate(id));
    }

    @Override
    public TimelineTemplateDTO resolve(Long majorTagId, String routeType) {
        TimelineTemplate t = resolveEntity(majorTagId, routeType);
        if (t == null) {
            throw new BusinessException(TimelineErrorCode.TEMPLATE_NOT_CONFIGURED,
                    "该专业该路线尚未配置可用的成长时间线模板");
        }
        return TimelineMapping.toTemplateDTO(t);
    }

    @Override
    public MajorTimelineStatsDTO getMajorTimelineStats(Long majorTagId, String routeType) {
        if (!RouteType.isValid(routeType)) {
            throw new BusinessException(TimelineErrorCode.ROUTE_TYPE_INVALID, "路线类型取值不合法");
        }
        MajorTimelineStatsDTO.MajorTimelineStatsDTOBuilder b = MajorTimelineStatsDTO.builder()
                .majorTagId(majorTagId).routeType(routeType);
        TimelineTemplate tpl = resolveEntity(majorTagId, routeType);
        if (tpl == null) {
            // 运营视角：未配置模板返回全零而非报错，便于仪表盘容错渲染
            return b.templateId(null).totalNodes(0).userCount(0).avgCompletion(0).build();
        }
        List<TimelineNode> nodes = nodeMapper.listByTemplate(tpl.getId());
        int totalNodes = nodes.size();
        b.templateId(tpl.getId()).totalNodes(totalNodes);
        if (totalNodes == 0) {
            return b.userCount(0).avgCompletion(0).build();
        }
        // 按用户聚合完成数
        Map<Long, Integer> doneByUser = new HashMap<>();
        for (UserProgress up : userProgressMapper.listByTemplate(tpl.getId())) {
            doneByUser.putIfAbsent(up.getUserId(), 0);
            if (ProgressStatus.DONE.name().equals(up.getStatus())) {
                doneByUser.merge(up.getUserId(), 1, Integer::sum);
            }
        }
        int userCount = doneByUser.size();
        int sumPct = 0;
        int bZero = 0, bLow = 0, bMid = 0, bFull = 0;
        for (int done : doneByUser.values()) {
            int pct = (int) Math.round(done * 100.0 / totalNodes);
            sumPct += pct;
            if (pct <= 0) {
                bZero++;
            } else if (pct <= 50) {
                bLow++;
            } else if (pct < 100) {
                bMid++;
            } else {
                bFull++;
            }
        }
        return b.userCount(userCount)
                .avgCompletion(userCount == 0 ? 0 : Math.round((float) sumPct / userCount))
                .bucketZero(bZero).bucketLow(bLow).bucketMid(bMid).bucketFull(bFull)
                .build();
    }

    /** §6.2 解析实体：专业专属优先，缺失回退全专业通用；均无则返回 null。 */
    private TimelineTemplate resolveEntity(Long majorTagId, String routeType) {
        if (majorTagId != null) {
            TimelineTemplate specific = templateMapper.findPublishedSpecific(majorTagId, routeType);
            if (specific != null) {
                return specific;
            }
        }
        return templateMapper.findPublishedGeneric(routeType);
    }

    private TimelineTemplate requireTemplate(Long id) {
        TimelineTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(TimelineErrorCode.RESOURCE_NOT_FOUND, "时间线模板不存在或已删除");
        }
        return t;
    }
}
