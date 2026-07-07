package com.xju.sem.module.social.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.social.dto.BadgeDTO;
import com.xju.sem.module.social.entity.UserBadge;
import com.xju.sem.module.social.mapper.UserBadgeMapper;
import com.xju.sem.module.social.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final UserBadgeMapper userBadgeMapper;

    @Override
    public List<BadgeDTO> listPublic(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        return userBadgeMapper.listPublic(userId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<BadgeDTO> listMine(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        return userBadgeMapper.listMine(userId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void setFlags(Long userId, Long badgeId, Boolean pinned, Boolean hidden) {
        if (userId == null || badgeId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "badgeId 不能为空");
        }
        UserBadge existing = userBadgeMapper.selectById(badgeId);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "徽章不存在");
        }
        if (!userId.equals(existing.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限操作他人徽章");
        }
        // 未传的字段沿用既有值，仅覆盖入参中显式给出的字段。
        Integer newPinned = pinned != null ? (pinned ? 1 : 0) : existing.getPinned();
        Integer newHidden = hidden != null ? (hidden ? 1 : 0) : existing.getHidden();
        userBadgeMapper.updateFlags(badgeId, userId, newPinned, newHidden);
    }

    private BadgeDTO toDTO(UserBadge b) {
        BadgeDTO dto = new BadgeDTO();
        dto.setId(b.getId());
        dto.setCode(b.getBadgeCode());
        dto.setName(b.getBadgeName());
        dto.setIcon(b.getIcon());
        dto.setPinned(b.getPinned() != null && b.getPinned() == 1);
        dto.setHidden(b.getHidden() != null && b.getHidden() == 1);
        dto.setAwardedAt(b.getAwardedAt());
        return dto;
    }
}
