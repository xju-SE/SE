package com.xju.sem.module.social.service.impl;

import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.social.dto.ConversationDTO;
import com.xju.sem.module.social.dto.MessageDTO;
import com.xju.sem.module.social.entity.Message;
import com.xju.sem.module.social.mapper.MessageMapper;
import com.xju.sem.module.social.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    /** 与 migration_social.sql message.content VARCHAR(2000) 对齐。 */
    private static final int CONTENT_MAX = 2000;

    private final MessageMapper messageMapper;

    @Override
    public MessageDTO send(Long senderId, Long receiverId, String content) {
        if (senderId == null || receiverId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "senderId/receiverId 不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "content 不能为空");
        }
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "不能给自己发私信");
        }

        Message m = new Message();
        m.setSenderId(senderId);
        m.setReceiverId(receiverId);
        m.setContent(content.length() <= CONTENT_MAX ? content : content.substring(0, CONTENT_MAX));
        m.setIsRead(0);
        messageMapper.insert(m);
        return toDTO(m);
    }

    @Override
    public List<ConversationDTO> conversations(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        return messageMapper.listConversations(userId);
    }

    @Override
    public List<MessageDTO> history(Long userId, Long peerId) {
        if (userId == null || peerId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "peerId 不能为空");
        }
        List<Message> list = messageMapper.listHistory(userId, peerId);
        messageMapper.markRead(userId, peerId);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void markRead(Long userId, Long peerId) {
        if (userId == null || peerId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "peerId 不能为空");
        }
        messageMapper.markRead(userId, peerId);
    }

    @Override
    public long unreadCount(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "userId 不能为空");
        }
        return messageMapper.countUnread(userId);
    }

    private MessageDTO toDTO(Message m) {
        MessageDTO dto = new MessageDTO();
        dto.setId(m.getId());
        dto.setSenderId(m.getSenderId());
        dto.setReceiverId(m.getReceiverId());
        dto.setContent(m.getContent());
        dto.setIsRead(m.getIsRead() != null && m.getIsRead() == 1);
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}
