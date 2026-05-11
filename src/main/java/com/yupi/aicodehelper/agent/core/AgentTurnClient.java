package com.yupi.aicodehelper.agent.core;

@FunctionalInterface
public interface AgentTurnClient {

    String nextTurn(String prompt);
}
