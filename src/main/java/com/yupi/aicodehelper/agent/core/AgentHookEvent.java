package com.yupi.aicodehelper.agent.core;

import java.util.Map;

public record AgentHookEvent(
        AgentHookEventType type,
        int turn,
        String toolName,
        Map<String, Object> payload
) {
    public static AgentHookEvent of(AgentHookEventType type, int turn, String toolName, Map<String, Object> payload) {
        return new AgentHookEvent(type, turn, toolName, payload == null ? Map.of() : Map.copyOf(payload));
    }
}
