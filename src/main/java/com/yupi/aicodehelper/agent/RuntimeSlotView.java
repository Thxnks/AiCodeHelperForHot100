package com.yupi.aicodehelper.agent;

import com.yupi.aicodehelper.agent.core.RuntimeSlotState;

import java.time.LocalDateTime;

public record RuntimeSlotView(
        String runtimeId,
        String taskId,
        String owner,
        Integer attempt,
        String executorId,
        String status,
        String stage,
        Integer progress,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime heartbeatAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime updatedAt
) {
    public static RuntimeSlotView from(RuntimeSlotState state) {
        if (state == null) {
            return null;
        }
        return new RuntimeSlotView(
                state.getRuntimeId(),
                state.getTaskId(),
                state.getOwner(),
                state.getAttempt(),
                state.getExecutorId(),
                state.getStatus().name(),
                state.getStage(),
                state.getProgress(),
                state.getErrorMessage(),
                state.getCreatedAt(),
                state.getHeartbeatAt(),
                state.getStartedAt(),
                state.getFinishedAt(),
                state.getUpdatedAt()
        );
    }
}
