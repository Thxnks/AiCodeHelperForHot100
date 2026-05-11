package com.yupi.aicodehelper.agent.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgentToolRegistry {

    private final Map<String, RegisteredAgentTool> tools = new LinkedHashMap<>();

    public AgentToolRegistry register(String name, String description, AgentToolHandler handler) {
        return register(name, description, AgentToolPermissionLevel.READ, handler);
    }

    public AgentToolRegistry register(String name,
                                      String description,
                                      AgentToolPermissionLevel permissionLevel,
                                      AgentToolHandler handler) {
        tools.put(name, new RegisteredAgentTool(new AgentToolSpec(name, description, permissionLevel), handler));
        return this;
    }

    public Optional<RegisteredAgentTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<AgentToolSpec> specs() {
        return tools.values().stream()
                .map(RegisteredAgentTool::spec)
                .toList();
    }

    public String describeAvailableTools() {
        StringBuilder builder = new StringBuilder();
        for (AgentToolSpec spec : specs()) {
            builder.append("- ")
                    .append(spec.name())
                    .append(" [")
                    .append(spec.permissionLevel())
                    .append("]")
                    .append(": ")
                    .append(spec.description())
                    .append("\n");
        }
        return builder.toString().trim();
    }
}
