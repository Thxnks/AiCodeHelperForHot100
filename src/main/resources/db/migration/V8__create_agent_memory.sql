CREATE TABLE IF NOT EXISTS agent_memory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    memory_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    scope VARCHAR(80) NOT NULL,
    subject VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    importance INT NOT NULL,
    source VARCHAR(80) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_memory_memory_id (memory_id),
    KEY idx_agent_memory_user_type_updated (user_id, type, updated_at),
    KEY idx_agent_memory_user_importance (user_id, importance, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
