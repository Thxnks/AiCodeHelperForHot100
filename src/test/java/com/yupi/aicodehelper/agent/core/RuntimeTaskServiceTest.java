package com.yupi.aicodehelper.agent.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeTaskServiceTest {

    @Test
    void shouldTrackMultipleRuntimeSlotsForOneTaskDefinition() {
        RuntimeTaskService service = new RuntimeTaskService(Runnable::run);

        RuntimeSlotState first = service.submit("task-1", "agent-worker", slot ->
                service.heartbeat(slot.getRuntimeId(), "first-attempt", 50));
        RuntimeSlotState second = service.submit("task-1", "agent-worker", slot ->
                service.heartbeat(slot.getRuntimeId(), "second-attempt", 50));

        assertThat(first.getAttempt()).isEqualTo(1);
        assertThat(second.getAttempt()).isEqualTo(2);
        assertThat(service.listByTaskId("task-1"))
                .extracting(RuntimeSlotState::getRuntimeId)
                .containsExactly(first.getRuntimeId(), second.getRuntimeId());
        assertThat(service.getLatestByTaskId("task-1"))
                .get()
                .extracting(RuntimeSlotState::getRuntimeId)
                .isEqualTo(second.getRuntimeId());
        assertThat(service.listByTaskId("task-1"))
                .extracting(RuntimeSlotState::getStatus)
                .containsExactly(RuntimeSlotStatus.SUCCESS, RuntimeSlotStatus.SUCCESS);
    }
}
