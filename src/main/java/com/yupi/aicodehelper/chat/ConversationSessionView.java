package com.yupi.aicodehelper.chat;

import com.yupi.aicodehelper.entity.ConversationSession;

import java.time.LocalDateTime;

public record ConversationSessionView(
        Long id,
        Integer memoryId,
        String roleId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ConversationSessionView from(ConversationSession session) {
        return new ConversationSessionView(
                session.getId(),
                session.getMemoryId(),
                session.getRoleId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
