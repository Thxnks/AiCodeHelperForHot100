package com.yupi.aicodehelper.agent.core;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AgentRecoveryPolicy {

    public AgentRecoveryDecision invalidModelOutput(String modelOutput, String reason) {
        return new AgentRecoveryDecision(
                AgentRecoveryType.INVALID_MODEL_OUTPUT,
                true,
                reason,
                Map.of(
                        "type", "recovery",
                        "error", "invalid_model_output",
                        "reason", reason,
                        "instruction", "Return exactly one JSON object using type=tool_use or type=final_answer.",
                        "previousOutput", modelOutput == null ? "" : modelOutput
                )
        );
    }

    public AgentRecoveryDecision unknownTool(ToolUseBlock toolUse) {
        return new AgentRecoveryDecision(
                AgentRecoveryType.UNKNOWN_TOOL,
                true,
                "Tool is not registered: " + toolUse.name(),
                Map.of(
                        "error", "unknown_tool",
                        "toolName", toolUse.name(),
                        "instruction", "Choose one of the available tools listed in the prompt, or return final_answer."
                )
        );
    }

    public AgentRecoveryDecision toolError(ToolUseBlock toolUse, Exception error) {
        String message = error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? "(no error message)"
                : error.getMessage();
        return new AgentRecoveryDecision(
                AgentRecoveryType.TOOL_ERROR,
                true,
                message,
                Map.of(
                        "error", "tool_error",
                        "toolName", toolUse.name(),
                        "message", message,
                        "instruction", "Use this error to choose a fallback tool, adjust the input, or return final_answer."
                )
        );
    }

    public AgentRecoveryDecision maxTurns(int maxTurns) {
        return new AgentRecoveryDecision(
                AgentRecoveryType.MAX_TURNS,
                false,
                "Agent stopped after reaching the maximum turn limit.",
                "Agent stopped after reaching the maximum turn limit (" + maxTurns + ")."
        );
    }
}
