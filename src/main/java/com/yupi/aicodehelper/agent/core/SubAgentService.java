package com.yupi.aicodehelper.agent.core;

import org.springframework.stereotype.Service;

@Service
public class SubAgentService {

    private final AgentLoopService agentLoopService;

    public SubAgentService(AgentLoopService agentLoopService) {
        this.agentLoopService = agentLoopService;
    }

    public SubAgentResult run(String task,
                              AgentToolRegistry toolRegistry,
                              AgentTurnClient turnClient,
                              int maxTurns) {
        AgentLoopState state = agentLoopService.run(
                """
                You are a focused sub-agent. Work in your own context and return only a concise summary for the parent agent.

                Sub-task:
                %s
                """.formatted(task).trim(),
                toolRegistry,
                turnClient,
                AgentLoopObserver.NOOP,
                maxTurns
        );
        return new SubAgentResult(state.finalAnswer(), state.turnCount());
    }
}
