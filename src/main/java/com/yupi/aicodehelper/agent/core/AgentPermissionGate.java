package com.yupi.aicodehelper.agent.core;

import org.springframework.stereotype.Service;

@Service
public class AgentPermissionGate {

    public AgentPermissionDecision check(RegisteredAgentTool tool, AgentPermissionContext context) {
        AgentPermissionContext safeContext = context == null ? AgentPermissionContext.readOnly() : context;
        AgentToolPermissionLevel level = tool.spec().permissionLevel();
        return switch (level) {
            case READ -> AgentPermissionDecision.allow();
            case WRITE -> safeContext.allowWrite()
                    ? AgentPermissionDecision.allow()
                    : AgentPermissionDecision.deny("WRITE tool requires allowWrite=true");
            case EXTERNAL -> safeContext.allowExternal()
                    ? AgentPermissionDecision.allow()
                    : AgentPermissionDecision.deny("EXTERNAL tool requires allowExternal=true");
            case SENSITIVE -> safeContext.allowSensitive()
                    ? AgentPermissionDecision.allow()
                    : AgentPermissionDecision.deny("SENSITIVE tool requires allowSensitive=true");
        };
    }
}
