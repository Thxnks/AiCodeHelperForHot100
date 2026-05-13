package com.yupi.aicodehelper.agent;

import com.yupi.aicodehelper.entity.AgentTask;

import java.time.LocalDateTime;
import java.util.List;

public record AgentTaskView(
        String taskId,
        String status,
        String goal,
        String finalAnswer,
        String errorMessage,
        RuntimeSlotView latestRuntime,
        List<RuntimeSlotView> runtimeHistory,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AgentStepView> steps
) {
    public static AgentTaskView from(AgentTask task,
                                     RuntimeSlotView latestRuntime,
                                     List<RuntimeSlotView> runtimeHistory,
                                     List<AgentStepView> steps) {
        return new AgentTaskView(
                task.getTaskId(),
                task.getStatus(),
                task.getGoal(),
                task.getFinalAnswer(),
                task.getErrorMessage(),
                latestRuntime,
                runtimeHistory,
                task.getCreatedAt(),
                task.getUpdatedAt(),
                steps
        );
    }
}
