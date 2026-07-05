package com.xju.sem.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.notification.dto.response.NotificationDTO;
import com.xju.sem.module.notification.entity.Notification;
import com.xju.sem.module.notification.enums.NotificationType;
import com.xju.sem.module.notification.mapper.NotificationMapper;
import com.xju.sem.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /** 与 schema.sql notification.title VARCHAR(100) 对齐。 */
    private static final int TITLE_MAX = 100;
    /** 与 schema.sql notification.content VARCHAR(500) 对齐。 */
    private static final int CONTENT_MAX = 500;
    /** 本期唯一投递渠道，PUSH 预留不投递（见 schema 列注释）。 */
    private static final String CHANNEL_INAPP = "INAPP";

    private final NotificationMapper notificationMapper;

    @Override
    public void send(Long userId, String type, String title, String content, String refType, Long refId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        if (!NotificationType.isValid(type)) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                    "type 取值不合法，须为 HELP_MATCH/ADOPT/AUDIT_RESULT/SYSTEM 之一：" + type);
        }
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "title/content 不能为空");
        }

        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        // 超长截断作为最后一道防线：各触发方模块拼接的通知文案理论上应自行控制长度，
        // 但本方法作为全局唯一落库入口，不应因某一调用方拼接超长文案而抛 SQL 异常拖垮其主流程。
        n.setTitle(truncate(title, TITLE_MAX));
        n.setContent(truncate(content, CONTENT_MAX));
        n.setRefType(refType);
        n.setRefId(refId);
        n.setIsRead(0);
        n.setChannel(CHANNEL_INAPP);
        notificationMapper.insert(n);
    }

    @Override
    public PageResult<NotificationDTO> pageList(Long userId, Boolean isRead, int page, int size) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId);
        if (isRead != null) {
            qw.eq(Notification::getIsRead, isRead ? 1 : 0);
        }
        qw.orderByDesc(Notification::getCreatedAt);

        IPage<Notification> p = notificationMapper.selectPage(pageOf(page, size), qw);
        List<NotificationDTO> records = p.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public void markRead(Long userId, Long id) {
        if (userId == null || id == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "id 不能为空");
        }
        int rows = notificationMapper.markRead(id, userId);
        if (rows == 0) {
            // 影响行数为 0 有两种可能：①该通知不存在/不属于当前用户；②本就已是已读态（无实际列变化）。
            // 二次查询区分：真正的"不存在/无权限"需要报错，"已是已读"应当幂等成功。
            Notification n = notificationMapper.selectById(id);
            if (n == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
            }
            if (!userId.equals(n.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权限操作他人通知");
            }
            // 到这里说明记录存在且属于本人，只是本来就是已读，幂等成功，不再抛异常。
        }
    }

    @Override
    public void markAllRead(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        notificationMapper.markAllRead(userId);
    }

    @Override
    public long countUnread(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        return notificationMapper.countUnread(userId);
    }

    private NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setType(n.getType());
        dto.setTitle(n.getTitle());
        dto.setContent(n.getContent());
        dto.setRefType(n.getRefType());
        dto.setRefId(n.getRefId());
        dto.setIsRead(n.getIsRead());
        dto.setChannel(n.getChannel());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }

    private Page<Notification> pageOf(int page, int size) {
        int p = page <= 0 ? 1 : page;
        int s = size <= 0 ? 10 : Math.min(size, 50);
        return new Page<>(p, s);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
