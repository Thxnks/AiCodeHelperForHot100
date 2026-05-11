package com.yupi.aicodehelper.agent.core;

public record AgentPermissionDecision(boolean allowed, String reason) {

    public static AgentPermissionDecision allow() {
        return new AgentPermissionDecision(true, "allowed");
    }

    public static AgentPermissionDecision deny(String reason) {
        return new AgentPermissionDecision(false, reason);
    }
}
