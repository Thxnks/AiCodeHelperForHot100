# PROJECT STATUS

## Snapshot (2026-04-26)
- Backend: Spring Boot 3.5 + LangChain4j + MySQL + Redis + Flyway
- Frontend: Vue 3 + Vite
- Core chat: SSE streaming from `/api/ai/chat`

## Delivered (Week1 ~ Week3)
- Auth and security chain:
  - access + refresh dual-token
  - `/auth/register` `/auth/login` `/auth/refresh` `/auth/logout` `/auth/me`
  - JWT auth filter + role parsing (`USER` / `ADMIN`)
  - refresh token persistence and revocation (`user_refresh_token`)
- Security and auditing:
  - protected APIs return 401/403 with unified error payload
  - trace id filter (`X-Trace-Id`)
  - audit interceptor logs `userId/path/status/cost/traceId`
- DB engineering:
  - Flyway migration baseline and versioned scripts
  - `ddl-auto=validate`
  - core tables for user/session/message/progress/token
- Hot100 module:
  - dataset + detail markdown
  - query APIs + learning progress persistence
  - weak tag / recommendation / study plan
  - async task submit + polling status APIs
- Cache and consistency:
  - Redis cache on hot read and recommendation APIs
  - progress write invalidation strategy
  - cache failure fallback via custom error handler

## Current Focus (Week4)
- Observability hardening:
  - actuator + micrometer metrics endpoint
  - trace/audit logs keep full request chain visibility
- Test hardening:
  - stabilize flaky or behavior-misaligned tests
  - build a fast regression command set
- Delivery:
  - Dockerfile + docker-compose + startup runbook

## Known Risks / Gaps
- Some integration tests are expensive and depend on external model/network behavior.
- Full local one-command environment (`docker compose up`) is not yet finalized.
- README and roadmap docs lagged behind code progress (now being updated in this iteration).
