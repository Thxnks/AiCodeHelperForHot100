package com.yupi.aicodehelper.agent;

public record AgentTraceSummaryView(
        Integer runtimeAttempts,
        Integer failedRuntimes,
        Integer totalSteps,
        Integer modelTurns,
        Integer toolCalls,
        Integer failedSteps,
        Long totalLatencyMs
) {
}
