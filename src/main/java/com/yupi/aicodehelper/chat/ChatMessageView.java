package com.yupi.aicodehelper.chat;

import com.yupi.aicodehelper.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageView(
        Long id,
        Integer memoryId,
        String sender,
        String roleId,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageView from(ChatMessage message) {
        return new ChatMessageView(
                message.getId(),
                message.getMemoryId(),
                message.getSender(),
                message.getRoleId(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
