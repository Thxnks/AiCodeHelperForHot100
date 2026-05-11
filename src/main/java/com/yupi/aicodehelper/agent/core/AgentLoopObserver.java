package com.yupi.aicodehelper.agent.core;

import java.util.Map;

public interface AgentLoopObserver {

    AgentLoopObserver NOOP = new AgentLoopObserver() {
    };

    default void onModelTurn(int turn, String input, String output, long latencyMs) {
    }

    default void onToolResult(int turn, String toolName, Map<String, Object> input, Object output, long latencyMs) {
    }

    default void onToolError(int turn, String toolName, Map<String, Object> input, Exception error, long latencyMs) {
    }
}
