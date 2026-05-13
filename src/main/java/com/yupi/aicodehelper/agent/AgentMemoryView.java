package com.yupi.aicodehelper.agent;

import com.yupi.aicodehelper.entity.AgentMemory;

import java.time.LocalDateTime;

public record AgentMemoryView(
        String memoryId,
        Long userId,
        String type,
        String scope,
        String subject,
        String content,
        Integer importance,
        String source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AgentMemoryView from(AgentMemory memory) {
        return new AgentMemoryView(
                memory.getMemoryId(),
                memory.getUserId(),
                memory.getType(),
                memory.getScope(),
                memory.getSubject(),
                memory.getContent(),
                memory.getImportance(),
                memory.getSource(),
                memory.getCreatedAt(),
                memory.getUpdatedAt()
        );
    }
}
