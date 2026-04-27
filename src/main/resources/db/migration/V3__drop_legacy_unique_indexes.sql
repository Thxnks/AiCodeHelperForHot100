SET @idx := (
    SELECT s.index_name
    FROM information_schema.statistics s
    WHERE s.table_schema = DATABASE()
      AND s.table_name = 'hot100_problem_progress'
      AND s.non_unique = 0
      AND s.index_name NOT IN ('PRIMARY', 'uk_hot100_user_problem')
    GROUP BY s.index_name
    HAVING COUNT(*) = 1 AND MAX(s.column_name = 'problem_slug') = 1
    LIMIT 1
);
SET @sql := IF(
    @idx IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE hot100_problem_progress DROP INDEX `', @idx, '`')
);
PREPARE s1 FROM @sql;
EXECUTE s1;
DEALLOCATE PREPARE s1;

SET @exists_idx := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'hot100_problem_progress'
      AND index_name = 'uk_hot100_user_problem'
);
SET @sql := IF(
    @exists_idx > 0,
    'SELECT 1',
    'ALTER TABLE hot100_problem_progress ADD CONSTRAINT uk_hot100_user_problem UNIQUE (user_id, problem_slug)'
);
PREPARE s2 FROM @sql;
EXECUTE s2;
DEALLOCATE PREPARE s2;

SET @idx := (
    SELECT s.index_name
    FROM information_schema.statistics s
    WHERE s.table_schema = DATABASE()
      AND s.table_name = 'conversation_session'
      AND s.non_unique = 0
      AND s.index_name NOT IN ('PRIMARY', 'uk_conversation_user_memory')
    GROUP BY s.index_name
    HAVING COUNT(*) = 1 AND MAX(s.column_name = 'memory_id') = 1
    LIMIT 1
);
SET @sql := IF(
    @idx IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE conversation_session DROP INDEX `', @idx, '`')
);
PREPARE s3 FROM @sql;
EXECUTE s3;
DEALLOCATE PREPARE s3;

SET @exists_idx := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'conversation_session'
      AND index_name = 'uk_conversation_user_memory'
);
SET @sql := IF(
    @exists_idx > 0,
    'SELECT 1',
    'ALTER TABLE conversation_session ADD CONSTRAINT uk_conversation_user_memory UNIQUE (user_id, memory_id)'
);
PREPARE s4 FROM @sql;
EXECUTE s4;
DEALLOCATE PREPARE s4;
