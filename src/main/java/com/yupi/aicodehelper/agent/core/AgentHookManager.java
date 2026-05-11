package com.yupi.aicodehelper.agent.core;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentHookManager {

    private final List<AgentHook> hooks;

    public AgentHookManager() {
        this(List.of());
    }

    public AgentHookManager(List<AgentHook> hooks) {
        this.hooks = List.copyOf(hooks);
    }

    public void publish(AgentHookEvent event) {
        for (AgentHook hook : hooks) {
            try {
                hook.handle(event);
            } catch (Exception ignored) {
                // Hooks are side effects and must not break the agent loop.
            }
        }
    }

    public List<AgentHook> hooks() {
        return new ArrayList<>(hooks);
    }
}
