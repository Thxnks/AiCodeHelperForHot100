CREATE TABLE IF NOT EXISTS ai_call_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_key VARCHAR(160) NULL,
    request_hash VARCHAR(64) NOT NULL,
    latency_ms BIGINT NULL,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    repaired BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ai_call_log_user_business_created (user_id, business_type, created_at),
    KEY idx_ai_call_log_request_hash (request_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
