package com.yupi.aicodehelper.agent;

import com.yupi.aicodehelper.entity.AgentStep;

import java.time.LocalDateTime;

public record AgentStepView(
        Integer stepOrder,
        String toolName,
        String toolInput,
        String toolOutput,
        String status,
        Long latencyMs,
        String errorMessage,
        LocalDateTime createdAt
) {
    public static AgentStepView from(AgentStep step) {
        return new AgentStepView(
                step.getStepOrder(),
                step.getToolName(),
                step.getToolInput(),
                step.getToolOutput(),
                step.getStatus(),
                step.getLatencyMs(),
                step.getErrorMessage(),
                step.getCreatedAt()
        );
    }
}
