package com.yupi.aicodehelper.agent;

import java.time.LocalDateTime;

public record AgentTraceEventView(
        String type,
        String label,
        String runtimeId,
        Integer stepOrder,
        String status,
        String stage,
        String toolName,
        Long latencyMs,
        String errorMessage,
        LocalDateTime timestamp,
        AgentStepView step
) {
    public static AgentTraceEventView runtime(String type,
                                              String label,
                                              RuntimeSlotView runtime,
                                              LocalDateTime timestamp) {
        return new AgentTraceEventView(
                type,
                label,
                runtime.runtimeId(),
                null,
                runtime.status(),
                runtime.stage(),
                null,
                null,
                runtime.errorMessage(),
                timestamp,
                null
        );
    }

    public static AgentTraceEventView step(AgentStepView step) {
        return new AgentTraceEventView(
                "STEP",
                step.toolName(),
                step.runtimeId(),
                step.stepOrder(),
                step.status(),
                null,
                step.toolName(),
                step.latencyMs(),
                step.errorMessage(),
                step.createdAt(),
                step
        );
    }
}
