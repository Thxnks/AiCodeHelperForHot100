package com.yupi.aicodehelper.agent.core;

public record AgentStreamEvent(
        String type,
        int turn,
        String toolName,
        String data,
        long latencyMs,
        String status
) {
}
