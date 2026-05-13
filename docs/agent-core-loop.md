# Agent Core Loop

This document records the first-stage Agent architecture used by the Hot100 learning agent.

## Scope

This stage implements the tutorial's core loop only:

- s01 Agent loop
- s02 Tool registry
- s03 In-session todo state
- s04 Lightweight sub-agent
- s05 Local skill loading
- s06 Current-run context compaction
- s07 Basic tool permission gate
- s08 Synchronous hook points
- s10 Prompt pipeline
- s11 Recovery policy
- s12 Task system
- s13 Background runtime task slots

It intentionally does not include long-term memory, scheduled agents, agent teams, worktree isolation, webhook/plugin hook execution, or MCP/plugin platform features.

## Runtime Loop

The main loop is implemented in `AgentLoopService`.

The execution shape is:

```text
messages
  -> model turn
  -> tool_use JSON
  -> tool registry dispatch
  -> tool_result message
  -> next model turn
  -> final_answer
```

The model must return one JSON object per turn:

```json
{"type":"tool_use","id":"toolu_x","name":"getWeakTags","input":{}}
```

or:

```json
{"type":"final_answer","content":"..."}
```

Tool results are appended back into `messages` as `tool_result` blocks. This is the important behavior: the next model turn sees real tool output before deciding what to do next.

## Tool Registry

Tools are registered through `AgentToolRegistry`.

The loop only knows:

- tool name
- tool description
- permission level
- handler

Hot100 business tools live in `Hot100AgentToolRegistry`. This keeps business behavior out of the loop implementation.

Core built-in tools are added by `AgentLoopService`:

- `todo_read`
- `todo_write`
- `list_skills`
- `load_skill`

## Permission Gate

Tools carry an `AgentToolPermissionLevel`:

- `READ`
- `WRITE`
- `EXTERNAL`
- `SENSITIVE`

`AgentLoopService` checks `AgentPermissionGate` before dispatching a tool.

Current default policy:

- `READ` is allowed
- `WRITE` requires `allowWrite=true`
- `EXTERNAL` requires `allowExternal=true`
- `SENSITIVE` requires `allowSensitive=true`

For Hot100 agent requests, `Hot100AgentRunRequest.allowWrite` controls write access. `updateProgress` and `analyzeWrongAnswer` are marked `WRITE`; read-only analysis and recommendation tools remain `READ`.

Permission denial is returned to the model as a `tool_result` with `permission_denied`, so the model can explain the denial or choose a read-only alternative.

## Hooks

`AgentHookManager` publishes synchronous in-process hook events.

Current event types:

- `BEFORE_MODEL_TURN`
- `AFTER_MODEL_TURN`
- `BEFORE_TOOL_CALL`
- `AFTER_TOOL_CALL`
- `ON_TOOL_ERROR`
- `ON_PERMISSION_DENIED`
- `ON_COMPACT`
- `ON_RECOVERY`

Hooks are side effects. Exceptions thrown by hooks are swallowed by `AgentHookManager` and must not break the agent loop.

This stage does not implement:

- user-defined scripts
- webhook delivery
- async event bus
- database-configured hooks
- plugin marketplace hooks

## Prompt Pipeline

Prompt assembly is handled by `AgentPromptBuilder`.

`AgentLoopService` passes an `AgentPromptContext` containing:

- current loop state
- current tool registry

The builder renders the model-facing prompt sections:

- agent role
- required JSON response format
- available tools and permission levels
- loop rules
- current todo state
- compact summary
- current messages

The loop keeps ownership of execution. The prompt builder only turns state into model input, which makes prompt changes testable without touching tool dispatch, permissions, hooks, or persistence.

## Recovery

Recovery behavior is centralized in `AgentRecoveryPolicy`.

Current recovery cases:

- invalid model output
- unknown tool
- tool handler exception
- maximum turn limit

Invalid model output no longer becomes a final answer immediately. The loop appends a recovery message telling the model to return one valid JSON object, then retries on the next turn.

Unknown tools and tool exceptions are returned as structured `tool_result` content. This keeps the loop alive and lets the next model turn choose a listed tool, adjust arguments, or produce a final answer.

Maximum turn recovery is terminal. The loop returns a clear final answer containing the configured max turn count.

Recovery events publish `ON_RECOVERY` through `AgentHookManager`.

## Task System

Durable task tracking is provided by `TaskGraphService`.

The loop now exposes task tools:

- `task_create`
- `task_update`
- `task_get`
- `task_list`

`TaskRecord` contains:

- `id`
- `subject`
- `description`
- `status`
- `blockedBy`
- `blocks`
- `owner`

Ready rule:

- `status == PENDING`
- `blockedBy` is empty

When a task is marked `COMPLETED`, dependent tasks are automatically unlocked by removing the completed task id from their `blockedBy`.

Current persistence strategy:

- Spring runtime: file-based `.tasks/task_<id>.json` via `FileTaskBoard`
- unit tests and local constructors: `InMemoryTaskBoard`

## Background Runtime Tasks

s13 adds runtime execution slots through `RuntimeTaskService`.

The boundary is:

- `TaskGraphService` / `TaskRecord`: durable description of work and dependencies
- `RuntimeTaskService` / `RuntimeSlotState`: in-process execution slot for who is running a task right now

`RuntimeSlotState` contains:

- `runtimeId`
- `taskId`
- `owner`
- `attempt`
- `executorId`
- `status`
- `stage`
- `progress`
- `errorMessage`
- `heartbeatAt`
- lifecycle timestamps

Current runtime statuses:

- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`

`Hot100AgentService.submit` now creates the durable `AgentTask` first, then submits the actual execution through `RuntimeTaskService`.

Task aggregate status now distinguishes queueing from execution:

- synchronous `/agent/hot100/run`: creates `AgentTask` as `RUNNING`
- background `/agent/hot100/tasks`: creates `AgentTask` as `QUEUED`
- when the runtime slot starts: `AgentTask` becomes `RUNNING`
- when the loop finishes: `AgentTask` becomes `SUCCESS` or `FAILED`

The `AgentTaskView` response includes `latestRuntime` and `runtimeHistory` blocks so clients can see both layers:

- persisted task status, final answer, and trace steps
- current runtime slot status and lifecycle stage

One task can now have multiple runtime slots, which leaves room for retry, executor failover, heartbeat timeout checks, and per-attempt trace isolation.

`AgentStep` includes `runtimeId` for background runs. This lets model/tool trace rows be grouped by a specific execution attempt instead of only by the task definition.

Runtime-specific trace can be queried through:

```text
GET /agent/hot100/tasks/{taskId}/runtimes/{runtimeId}/steps
```

This keeps the s13 concept explicit: a task describes what should be done; a runtime slot describes one execution attempt.

## Todo State

Todo state is stored in `AgentLoopState`.

It is deliberately in-session only:

- no database table
- no durable task graph
- no dependency model

The todo list exists to help the model keep a short plan during one run.

## Sub-Agent

`SubAgentService` runs a focused loop with its own messages and a limited tool registry.

The parent agent receives only:

- summary
- turn count

The parent does not inherit the child messages. This keeps the parent context compact.

Current Hot100 sub-agent tools:

- `analyzeProblemWithSubAgent`
- `reviewWrongAnswerWithSubAgent`

## Skills

Skills are local markdown files under:

```text
src/main/resources/skills/<skill-name>/SKILL.md
```

The loop exposes:

- `list_skills`
- `load_skill`

The prompt only advertises the capability. Full skill content is loaded only when the model asks for it.

Current skills:

- `hot100-review`
- `wrong-answer-analysis`
- `interview-coach`

## Context Compaction

Compaction is current-run only. It is not long-term memory.

`AgentLoopService` compacts when either condition is met:

- too many messages
- rendered message content is too long

The compact summary preserves:

- turn count
- todo state
- recent observations

After compaction, the loop keeps the original user message and a compact summary message.

## Trace

`Hot100AgentService` stores execution trace in `agent_step`.

Trace names are normalized:

- `model_turn`
- `tool:<toolName>`

Examples:

- `tool:todo_write`
- `tool:getWeakTags`
- `tool:load_skill`

`AgentTask` and `AgentStep` are trace records only in this stage. They are not a durable task system.

## Verification

Core behavior is covered by:

- `AgentLoopServiceTest`
- `AgentPromptBuilderTest`
- `Hot100AgentServiceTest`

The tests verify:

- tool results flow into following model turns
- todo state is visible after `todo_write`
- skills can be loaded through tools
- long contexts compact
- sub-agents run in independent context
- write tools are denied without permission and allowed with explicit write permission
- hooks are published without changing the main loop behavior
- prompt sections include tools, permission levels, todos, compact summary, and messages
- invalid model output, unknown tools, tool errors, and max-turn stops use recovery policy
- persistent task records support dependency tracking and auto-unlock on completion
- submitted Hot100 agent tasks expose runtime slot state for background execution
- background agent tasks can remain `QUEUED` until their runtime slot starts
- runtime-specific step queries return trace rows for one execution attempt
- `Hot100AgentService.run` persists final answer and trace steps
