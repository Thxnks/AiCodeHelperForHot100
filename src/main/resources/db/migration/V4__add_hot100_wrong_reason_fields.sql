SET @stmt = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'hot100_problem_progress'
              AND column_name = 'wrong_reason'
        ),
        'SELECT 1',
        'ALTER TABLE hot100_problem_progress ADD COLUMN wrong_reason TEXT NULL'
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
              AND table_name = 'hot100_problem_progress'
              AND column_name = 'knowledge_point'
        ),
        'SELECT 1',
        'ALTER TABLE hot100_problem_progress ADD COLUMN knowledge_point VARCHAR(255) NULL'
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
              AND table_name = 'hot100_problem_progress'
              AND column_name = 'ai_feedback'
        ),
        'SELECT 1',
        'ALTER TABLE hot100_problem_progress ADD COLUMN ai_feedback TEXT NULL'
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
              AND table_name = 'hot100_problem_progress'
              AND column_name = 'next_action'
        ),
        'SELECT 1',
        'ALTER TABLE hot100_problem_progress ADD COLUMN next_action TEXT NULL'
    )
);
PREPARE s4 FROM @stmt;
EXECUTE s4;
DEALLOCATE PREPARE s4;
