# Week2 Database Engineering Notes

## Migration Strategy

- Use Flyway as the single migration entry.
- Baseline existing environments with `baseline-on-migrate=true`.
- Add versioned SQL scripts under `src/main/resources/db/migration`.
- Set `spring.jpa.hibernate.ddl-auto=validate` to prevent runtime schema drift.

## Index Design

- `user_account`
- `uk_user_account_username` for login lookup.
- `uk_user_account_email` for uniqueness and registration checks.

- `user_refresh_token`
- `uk_user_refresh_token_token_id` for refresh token validation.
- `idx_user_refresh_token_user_id` for user-level token revocation.
- `idx_user_refresh_token_expires_at` for scheduled cleanup.

- `conversation_session`
- `uk_conversation_user_memory` to avoid duplicate memory IDs per user.
- `idx_conversation_session_updated_at` for latest-session queries.

- `chat_message`
- `idx_chat_message_memory_id_created_at` for message timeline queries.
- `idx_chat_message_user_id_created_at` for user-scoped history.

- `hot100_problem_progress`
- `uk_hot100_user_problem` to enforce one progress row per user/problem.
- `idx_hot100_progress_user_status_updated` for wrong-book/recommendation queries.
- `idx_hot100_progress_updated_at` for global maintenance and reporting queries.

## Slow Query Analysis Checklist

1. Enable MySQL slow query log:
```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.2;
SET GLOBAL log_queries_not_using_indexes = 'ON';
```

2. Sample inspection command:
```sql
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
```

3. For each slow SQL:
- Run `EXPLAIN ANALYZE`.
- Confirm index hit ratio and rows scanned.
- Add composite index only if query shape is stable and high-frequency.
- Re-run benchmark after index change.

4. Keep cleanup task for `user_refresh_token` expired rows to control index bloat.
