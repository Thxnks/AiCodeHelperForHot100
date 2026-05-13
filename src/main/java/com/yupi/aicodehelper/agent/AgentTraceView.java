package com.yupi.aicodehelper.agent;

import com.yupi.aicodehelper.entity.AgentTask;

import java.time.LocalDateTime;
import java.util.List;

public record AgentTraceView(
        String taskId,
        String status,
        String goal,
        String finalAnswer,
        String errorMessage,
        RuntimeSlotView latestRuntime,
        List<RuntimeSlotView> runtimes,
        AgentTraceSummaryView summary,
        List<AgentTraceEventView> timeline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AgentTraceView from(AgentTask task,
                                      RuntimeSlotView latestRuntime,
                                      List<RuntimeSlotView> runtimes,
                                      AgentTraceSummaryView summary,
                                      List<AgentTraceEventView> timeline) {
        return new AgentTraceView(
                task.getTaskId(),
                task.getStatus(),
                task.getGoal(),
                task.getFinalAnswer(),
                task.getErrorMessage(),
                latestRuntime,
                runtimes,
                summary,
                timeline,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
