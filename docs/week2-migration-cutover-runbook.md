# Week2 Migration Cutover Runbook

## Goal

Switch from Hibernate auto-DDL to Flyway-managed schema evolution without breaking existing environments.

## Cutover Steps

1. Backup production/staging database.
2. Deploy application build containing Flyway scripts.
3. Keep `spring.flyway.baseline-on-migrate=true` for the first cutover release.
4. Start service and verify Flyway metadata table `flyway_schema_history`.
5. Confirm application starts with `spring.jpa.hibernate.ddl-auto=validate`.

## Verification Checklist

1. `flyway_schema_history` has baseline record and migration records (`V1`, `V2`).
2. Core tables exist: `user_account`, `user_refresh_token`, `conversation_session`, `chat_message`, `hot100_problem_progress`.
3. Key unique indexes exist:
- `uk_user_account_username`
- `uk_user_account_email`
- `uk_conversation_user_memory`
- `uk_hot100_user_problem`

## Rollback Strategy

1. If startup fails before serving traffic:
- Roll back to previous application version.
- Keep database unchanged (Flyway migrations are forward-only; do not manually delete migration history in prod).

2. If startup succeeds but behavior regresses:
- Use hotfix migration (`Vx__hotfix_*.sql`) to correct schema/data.
- Re-deploy fixed application version.

3. Emergency only:
- Restore from backup to pre-cutover snapshot.
- Revert application version.

## Post-Cutover Hardening

1. After all environments are migrated, set `baseline-on-migrate=false`.
2. Enforce migration review in PR checklist:
- migration SQL reviewed
- rollback path documented
- `EXPLAIN` result attached for new index changes
