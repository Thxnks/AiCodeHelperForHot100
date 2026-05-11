package com.yupi.aicodehelper.agent.core;

public record AgentRecoveryDecision(
        AgentRecoveryType type,
        boolean retry,
        String reason,
        Object content
) {
}
