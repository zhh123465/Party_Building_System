package com.party.build.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.party.build.audit.domain.Message;
import com.party.build.audit.mapper.MessageMapper;
import com.party.build.auth.domain.SysUser;
import com.party.build.auth.mapper.SysUserMapper;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.api.PageResult;
import com.party.build.common.exception.BizException;
import com.party.build.verify.domain.Material;
import com.party.build.verify.domain.MaterialVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageMapper messageMapper;
    private final SysUserMapper userMapper;

    public void send(Long receiverId, String type, String title, String content, String bizType, Long bizId) {
        if (receiverId == null) {
            return;
        }
        Message message = new Message();
        message.setReceiverId(receiverId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setBizType(bizType);
        message.setBizId(bizId);
        messageMapper.insert(message);
    }

    public void sendReviewResult(Material material, MaterialVersion version, String conclusion, String opinion) {
        Long receiver = version.getUploadedBy();
        if (receiver == null) {
            SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPersonId, material.getPersonId())
                    .eq(SysUser::getRoleCode, "R4")
                    .last("limit 1"));
            receiver = user == null ? null : user.getUserId();
        }
        if ("RETURN".equals(conclusion)) {
            send(receiver, "REVIEW_RETURN", "材料被退回，请整改后重新上传",
                    "退回意见：" + (opinion == null ? "请按材料要求补充完善" : opinion),
                    "MATERIAL", material.getMaterialId());
        } else {
            send(receiver, "REVIEW_PASS", "材料复核通过",
                    "材料已通过复核，无需再修改。", "MATERIAL", material.getMaterialId());
        }
    }

    public PageResult<Message> list(Long receiverId, long page, long size) {
        Page<Message> p = new Page<>(page, size);
        Page<Message> result = messageMapper.selectPage(p, new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .orderByDesc(Message::getMessageId));
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    public long unreadCount(Long receiverId) {
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .isNull(Message::getReadAt));
        return count == null ? 0 : count;
    }

    public void markRead(Long messageId, Long receiverId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null || !message.getReceiverId().equals(receiverId)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "消息不存在");
        }
        if (message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
            messageMapper.updateById(message);
        }
    }

    public void markAllRead(Long receiverId) {
        for (Message message : messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .isNull(Message::getReadAt))) {
            message.setReadAt(LocalDateTime.now());
            messageMapper.updateById(message);
        }
    }
}