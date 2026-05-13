ALTER TABLE agent_step
    ADD COLUMN runtime_id VARCHAR(80) NULL AFTER task_id,
    ADD KEY idx_agent_step_runtime_order (runtime_id, step_order);
