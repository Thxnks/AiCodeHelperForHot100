CREATE TABLE IF NOT EXISTS agent_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    goal TEXT NOT NULL,
    status VARCHAR(24) NOT NULL,
    final_answer TEXT NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_task_task_id (task_id),
    KEY idx_agent_task_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_step (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    step_order INT NOT NULL,
    tool_name VARCHAR(120) NOT NULL,
    tool_input TEXT NULL,
    tool_output TEXT NULL,
    status VARCHAR(24) NOT NULL,
    latency_ms BIGINT NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_agent_step_task_order (task_id, step_order),
    KEY idx_agent_step_tool_created (tool_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
