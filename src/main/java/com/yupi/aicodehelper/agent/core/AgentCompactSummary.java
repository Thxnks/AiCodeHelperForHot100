package com.yupi.aicodehelper.agent.core;

public record AgentCompactSummary(String content, int turnsCompleted, String tier) {

    public static AgentCompactSummary snip(String summary, int turns) {
        return new AgentCompactSummary(summary, turns, "TIER1_SNIP");
    }

    public static AgentCompactSummary microcompact(String summary, int turns) {
        return new AgentCompactSummary(summary, turns, "TIER2_MICROCOMPACT");
    }

    public static AgentCompactSummary autocompact(String summary, int turns) {
        return new AgentCompactSummary(summary, turns, "TIER3_AUTOCOMPACT");
    }
}
