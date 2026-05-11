package com.yupi.aicodehelper.agent.core;

@FunctionalInterface
public interface AgentHook {

    void handle(AgentHookEvent event);
}
