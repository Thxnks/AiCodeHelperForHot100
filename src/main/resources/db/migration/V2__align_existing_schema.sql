SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_account'
              AND column_name = 'role'
        ),
        'SELECT 1',
        'ALTER TABLE user_account ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT ''USER'''
    )
);
PREPARE s1 FROM @stmt;
EXECUTE s1;
DEALLOCATE PREPARE s1;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_account'
              AND column_name = 'token_version'
        ),
        'SELECT 1',
        'ALTER TABLE user_account ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0'
    )
);
PREPARE s2 FROM @stmt;
EXECUTE s2;
DEALLOCATE PREPARE s2;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'conversation_session'
              AND column_name = 'user_id'
        ),
        'SELECT 1',
        'ALTER TABLE conversation_session ADD COLUMN user_id BIGINT NULL'
    )
);
PREPARE s3 FROM @stmt;
EXECUTE s3;
DEALLOCATE PREPARE s3;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'chat_message'
              AND column_name = 'user_id'
        ),
        'SELECT 1',
        'ALTER TABLE chat_message ADD COLUMN user_id BIGINT NULL'
    )
);
PREPARE s4 FROM @stmt;
EXECUTE s4;
DEALLOCATE PREPARE s4;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'hot100_problem_progress'
              AND column_name = 'user_id'
        ),
        'SELECT 1',
        'ALTER TABLE hot100_problem_progress ADD COLUMN user_id BIGINT NULL'
    )
);
PREPARE s5 FROM @stmt;
EXECUTE s5;
DEALLOCATE PREPARE s5;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_refresh_token'
              AND column_name = 'user_id'
        ),
        'SELECT 1',
        'ALTER TABLE user_refresh_token ADD COLUMN user_id BIGINT NOT NULL'
    )
);
PREPARE s6 FROM @stmt;
EXECUTE s6;
DEALLOCATE PREPARE s6;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_refresh_token'
              AND column_name = 'token_id'
        ),
        'SELECT 1',
        'ALTER TABLE user_refresh_token ADD COLUMN token_id VARCHAR(64) NOT NULL'
    )
);
PREPARE s7 FROM @stmt;
EXECUTE s7;
DEALLOCATE PREPARE s7;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_refresh_token'
              AND column_name = 'token_hash'
        ),
        'SELECT 1',
        'ALTER TABLE user_refresh_token ADD COLUMN token_hash VARCHAR(128) NOT NULL'
    )
);
PREPARE s8 FROM @stmt;
EXECUTE s8;
DEALLOCATE PREPARE s8;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_refresh_token'
              AND column_name = 'expires_at'
        ),
        'SELECT 1',
        'ALTER TABLE user_refresh_token ADD COLUMN expires_at DATETIME(6) NOT NULL'
    )
);
PREPARE s9 FROM @stmt;
EXECUTE s9;
DEALLOCATE PREPARE s9;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_refresh_token'
              AND column_name = 'revoked_at'
        ),
        'SELECT 1',
        'ALTER TABLE user_refresh_token ADD COLUMN revoked_at DATETIME(6) NULL'
    )
);
PREPARE s10 FROM @stmt;
EXECUTE s10;
DEALLOCATE PREPARE s10;

SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_refresh_token'
              AND column_name = 'created_at'
        ),
        'SELECT 1',
        'ALTER TABLE user_refresh_token ADD COLUMN created_at DATETIME(6) NOT NULL'
    )
);
PREPARE s11 FROM @stmt;
EXECUTE s11;
DEALLOCATE PREPARE s11;
