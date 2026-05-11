package com.yupi.aicodehelper.agent.core;

public enum AgentHookEventType {
    BEFORE_MODEL_TURN,
    AFTER_MODEL_TURN,
    BEFORE_TOOL_CALL,
    AFTER_TOOL_CALL,
    ON_TOOL_ERROR,
    ON_PERMISSION_DENIED,
    ON_COMPACT,
    ON_RECOVERY
}
