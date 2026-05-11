package com.yupi.aicodehelper.agent.core;

public record AgentPermissionContext(boolean allowWrite, boolean allowExternal, boolean allowSensitive) {

    public static AgentPermissionContext readOnly() {
        return new AgentPermissionContext(false, false, false);
    }
}
