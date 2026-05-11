package com.yupi.aicodehelper.agent.core;

public record AgentPromptContext(
        AgentLoopState state,
        AgentToolRegistry toolRegistry
) {
}
