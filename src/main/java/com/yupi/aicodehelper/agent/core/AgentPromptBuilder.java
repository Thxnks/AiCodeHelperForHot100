package com.yupi.aicodehelper.agent.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AgentPromptBuilder {

    private final ObjectMapper objectMapper;

    public AgentPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(AgentPromptContext context) {
        return """
                You are a Hot100 algorithm practice agent.

                You must respond with one JSON object only.

                Available tools:
                %s

                Response formats:
                {"type":"tool_use","id":"toolu_x","name":"getWeakTags","input":{}}
                {"type":"final_answer","content":"final answer to the user"}

                Rules:
                - Use tools when you need real Hot100 progress, problem, weak-tag, recommendation, or knowledge data.
                - Use todo_write to maintain a short plan for multi-step work, and todo_read when you need to inspect it.
                - Use task_create/task_update/task_get/task_list for durable cross-turn work tracking and dependency management.
                - Use list_skills and load_skill when a local skill would improve the answer.
                - After receiving a tool_result, use it to decide the next step.
                - After receiving a recovery message, correct the previous issue and continue.
                - Do not call tools that are not listed.
                - Prefer a final_answer once you have enough observations.

                Current todos:
                %s

                Compact summary:
                %s

                Messages:
                %s
                """.formatted(
                context.toolRegistry().describeAvailableTools(),
                renderTodos(context.state()),
                renderCompactSummary(context.state()),
                renderMessages(context.state())
        ).trim();
    }

    public String renderTodos(AgentLoopState state) {
        if (state.todos().isEmpty()) {
            return "[]";
        }
        return toJson(state.todos());
    }

    public String renderMessages(AgentLoopState state) {
        StringBuilder builder = new StringBuilder();
        for (AgentMessage message : state.messages()) {
            builder.append(message.role())
                    .append(": ")
                    .append(message.content())
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private String renderCompactSummary(AgentLoopState state) {
        return state.compactSummary() == null ? "(none)" : state.compactSummary().content();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
