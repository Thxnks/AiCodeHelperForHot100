# Week3 Cache and Consistency

## Scope

- Redis cache for hot100 problem read APIs.
- Redis cache for user recommendation and study plan.
- Consistency strategy on progress updates.

## Cache Keys

- `hot100:problem:list`:
  key = `keyword|tag|difficulty`
- `hot100:problem:detail`:
  key = `slug`
- `hot100:tag:list`:
  key = default spring key
- `hot100:recommendation`:
  key = `userId|limit`
- `hot100:studyPlan`:
  key = `userId|days`
- `hot100:tagMastery`:
  key = `userId`

## TTL Strategy

- Problem/tags read cache: 300s (mostly static data)
- Recommendation/studyPlan/tagMastery: 180s (user behavior sensitive)

## Consistency Strategy

- On `upsert` progress:
  - clear all recommendation cache entries (user limits vary)
  - clear all study plan cache entries (days vary)
  - evict user tagMastery cache by `userId`
- Trade-off:
  - slightly coarse invalidation
  - guaranteed freshness after progress writes

## Failure Strategy

- Cache errors are swallowed and logged by custom `CacheErrorHandler`.
- Business flow falls back to database/in-memory computation and remains available.
