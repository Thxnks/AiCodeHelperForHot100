package com.yupi.aicodehelper.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptBuilderTest {

    @Test
    void shouldRenderPromptSectionsFromStateAndTools() {
        AgentPromptBuilder promptBuilder = new AgentPromptBuilder(new ObjectMapper());
        AgentLoopState state = new AgentLoopState("Recommend my next Hot100 problem.");
        state.replaceTodos(List.of(new TodoItem("Inspect weak tags", TodoStatus.IN_PROGRESS)));
        state.messages().add(new AgentMessage("assistant", "Previous observation"));
        state.compact("Earlier turns were summarized.", 3, "TIER3_AUTOCOMPACT");
        AgentToolRegistry registry = new AgentToolRegistry()
                .register("getWeakTags", "Inspect weak tags.", AgentToolPermissionLevel.READ, input -> "dp")
                .register("updateProgress", "Update practice progress.", AgentToolPermissionLevel.WRITE,
                        input -> "saved");

        String prompt = promptBuilder.build(new AgentPromptContext(state, registry));

        assertThat(prompt)
                .contains("Available tools:")
                .contains("getWeakTags [READ]")
                .contains("updateProgress [WRITE]")
                .contains("Response formats:")
                .contains("\"type\":\"tool_use\"")
                .contains("Current todos:")
                .contains("Inspect weak tags")
                .contains("Compact summary:")
                .contains("Tier: TIER3_AUTOCOMPACT")
                .contains("at turn 3")
                .contains("Earlier turns were summarized.")
                .contains("Messages:")
                .contains("Recommend my next Hot100 problem.");
    }
}
