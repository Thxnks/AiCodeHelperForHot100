# Week3 Async Task Design

## Scope

- Move heavy recommendation and study-plan generation to async tasks.
- Keep existing sync APIs unchanged for backward compatibility.

## Endpoints

- `POST /hot100/tasks/recommendations?limit=5`
- `POST /hot100/tasks/study-plan?days=7`
- `GET /hot100/tasks/{taskId}`

## Execution Model

- Thread pool: `core=4`, `max=8`, `queue=100`.
- Task lifecycle:
  - `PENDING`
  - `RUNNING`
  - `SUCCESS` or `FAILED`

## Storage Model

- In-memory `ConcurrentHashMap<taskId, taskState>`.
- Simple retention:
  - keep up to 500 tasks
  - prune completed tasks down to 300 when threshold reached

## Trade-offs

- Pros:
  - quick to implement
  - no infrastructure dependency
  - suitable for single-node interview project
- Cons:
  - task state lost on restart
  - multi-instance requires shared storage or message queue
