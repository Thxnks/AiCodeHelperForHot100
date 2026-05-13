package com.yupi.aicodehelper.agent;

import com.yupi.aicodehelper.entity.AgentMemory;
import com.yupi.aicodehelper.repository.AgentMemoryRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentMemoryServiceTest {

    @Test
    void shouldRememberRecallAndSummarizeUserMemory() {
        AgentMemoryRepository repository = mock(AgentMemoryRepository.class);
        List<AgentMemory> memories = new ArrayList<>();
        when(repository.save(any(AgentMemory.class))).thenAnswer(invocation -> {
            AgentMemory memory = invocation.getArgument(0);
            memories.add(memory);
            return memory;
        });
        when(repository.findByUserIdOrderByImportanceDescUpdatedAtDesc(1L)).thenAnswer(invocation -> memories.stream()
                .sorted(Comparator.comparing(AgentMemory::getImportance).reversed())
                .toList());
        when(repository.findTop20ByUserIdOrderByImportanceDescUpdatedAtDesc(1L)).thenAnswer(invocation -> memories.stream()
                .sorted(Comparator.comparing(AgentMemory::getImportance).reversed())
                .limit(20)
                .toList());

        AgentMemoryService service = new AgentMemoryService(repository);

        service.rememberProgress(
                1L,
                "coin-change",
                "WRONG",
                "Need to review dp state definition",
                "完全背包 DP",
                "Updated dp array in the wrong order",
                "Practice coin-change again with boundary cases"
        );

        List<AgentMemoryView> recalled = service.recall(1L, "coin change dp wrong order", 5);
        AgentMemoryProfileView profile = service.profile(1L);

        assertThat(recalled)
                .extracting(AgentMemoryView::type)
                .contains(AgentMemoryType.WRONG_ANSWER.name(), AgentMemoryType.WEAKNESS.name());
        assertThat(recalled)
                .anySatisfy(memory -> assertThat(memory.content()).contains("wrong order"));
        assertThat(profile.weaknesses())
                .anySatisfy(memory -> assertThat(memory).contains("完全背包 DP"));
        assertThat(profile.nextActions())
                .anySatisfy(memory -> assertThat(memory).contains("Practice coin-change"));
    }
}
