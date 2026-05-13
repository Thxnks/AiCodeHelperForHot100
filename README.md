# AI Code Helper For Hot100

[中文文档](./README.zh-CN.md)

AI Code Helper is a Spring Boot based Agent Runtime project for Hot100 algorithm learning. It is not a thin LLM API wrapper: the backend controls tool execution, permissions, task runtime slots, trace persistence, and recovery while the model decides the next reasoning step through structured `tool_use` JSON.

## Project Positioning

This repository is designed to demonstrate backend engineering plus AI Agent application development:

- Backend foundation: Spring Boot 3.5, Java 21, Spring Security, JWT, JPA, Flyway, Redis, RabbitMQ, Docker Compose.
- Agent Runtime: multi-turn loop, tool registry, tool result feedback, todo state, skills, sub-agents, compaction, permissions, hooks, recovery, task graph, background runtime slots.
- Hot100 learning domain: progress analytics, weak-tag analysis, wrong-answer diagnosis, recommendations, study plans, problem search, and knowledge retrieval.

## Agent Runtime Architecture

```text
User Goal
  -> AgentTask
  -> RuntimeSlot
  -> AgentLoopService
       -> AgentPromptBuilder
       -> model turn
       -> tool_use JSON
       -> AgentPermissionGate
       -> AgentToolRegistry
       -> Java tool handler
       -> tool_result message
       -> next model turn
       -> final_answer
  -> AgentStep trace
```

The model is allowed to choose tools, but it cannot execute arbitrary code. Every tool must be registered by the backend with a name, description, permission level, and Java handler.

## s12/s13 Task Runtime Model

The project separates task definition from runtime execution attempts:

```text
AgentTask
  taskId, userId, goal, aggregate status, finalAnswer
      |
      | 1:N
      v
RuntimeSlot
  runtimeId, attempt, executorId, status, stage, progress, heartbeatAt
      |
      | 1:N
      v
AgentStep
  runtimeId, stepOrder, model/tool input, output, status, latency
```

Status flow:

```text
POST /agent/hot100/tasks
  -> AgentTask QUEUED
  -> RuntimeSlot PENDING
  -> RuntimeSlot RUNNING
  -> AgentTask RUNNING
  -> model_turn / tool_call trace rows
  -> RuntimeSlot SUCCESS or FAILED
  -> AgentTask SUCCESS or FAILED
```

This makes retries and future executor failover possible: one task can have multiple runtime slots, and each slot owns its own trace rows.

## Agent Capabilities

- ReAct-style loop: `messages -> model -> tool_use -> tool_result -> next model turn -> final_answer`
- Tool registry with permission levels: `READ`, `WRITE`, `EXTERNAL`, `SENSITIVE`
- Built-in tools: `todo_read`, `todo_write`, `list_skills`, `load_skill`, `task_create`, `task_update`, `task_get`, `task_list`
- Hot100 tools: progress lookup, weak tags, tag mastery, wrong book, recommendation, study plan, problem detail, knowledge retrieval, guarded progress update
- Runtime observability: `agent_task`, `runtimeHistory`, `latestRuntime`, `agent_step`
- Recovery policy for invalid model output, unknown tools, tool handler exceptions, and max-turn stops
- Hook events for model turns, tool calls, permission denial, compaction, and recovery
- Sub-agent support for focused problem analysis and wrong-answer review

## Main APIs

Hot100:

- `GET /api/hot100/problems`
- `GET /api/hot100/problems/{slug}`
- `POST /api/hot100/progress`
- `GET /api/hot100/tag-mastery`
- `GET /api/hot100/wrong-book/analysis`
- `POST /api/hot100/wrong-book/analyze`
- `GET /api/hot100/ai-recommendations`

Agent:

- `POST /api/agent/hot100/run`
- `POST /api/agent/hot100/tasks`
- `GET /api/agent/hot100/tasks/{taskId}`
- `GET /api/agent/hot100/tasks/{taskId}/steps`
- `GET /api/agent/hot100/tasks/{taskId}/runtimes/{runtimeId}/steps`

Chat:

- `GET /api/ai/chat`

## Tech Stack

- Backend: `Java 21`, `Spring Boot 3.5`, `Spring Security`, `Spring Data JPA`, `Flyway`
- AI: `LangChain4j`, `DashScope/Qwen`
- Frontend: `Vue 3`, `Vite`, `Axios`, `SSE`
- Storage and middleware: `MySQL 8`, `Redis`, `RabbitMQ`
- Deployment: `Docker`, `Docker Compose`

## Quick Start

```powershell
Copy-Item .env.example .env
Copy-Item ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

Configure at least:

- `DASHSCOPE_API_KEY`
- `APP_AUTH_JWT_SECRET`
- MySQL / Redis / RabbitMQ connection settings

Start with Docker:

```bash
docker compose up -d --build
```

Local backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Local frontend:

```powershell
cd ai-code-helper-frontend
npm install
npm run dev
```

## Verification

Run the focused Agent regression suite:

```powershell
.\mvnw.cmd test "-Dtest=RuntimeTaskServiceTest,Hot100AgentServiceTest,AgentLoopServiceTest,AgentPromptBuilderTest"
```

Run all backend tests:

```powershell
.\mvnw.cmd test
```

Build frontend:

```powershell
cd ai-code-helper-frontend
npm run build
```

## Resume Description

Designed and implemented an AI Agent Runtime for Hot100 algorithm learning. The backend supports a multi-turn model/tool loop, backend-controlled tool registry, tool-result feedback, permission-gated write tools, local skills, sub-agents, context compaction, hook events, structured recovery, persistent task graph, background runtime slots, and per-runtime execution trace.

Built the Hot100 business tool layer on top of the Agent Runtime, covering progress lookup, weak-tag analysis, wrong-answer diagnosis, recommendation generation, study-plan generation, problem search, knowledge retrieval, and guarded progress updates. The system keeps model reasoning flexible while keeping execution controlled, observable, testable, and permission-aware.

## Project Structure

```text
.
|-- ai-code-helper-frontend/
|-- src/main/java/com/yupi/aicodehelper/
|   |-- agent/
|   |   |-- core/
|   |   |-- Hot100AgentService.java
|   |   `-- Hot100AgentToolRegistry.java
|   |-- controller/
|   |-- hot100/
|   |-- ai/
|   |-- auth/
|   |-- entity/
|   `-- repository/
|-- src/main/resources/
|   |-- db/migration/
|   |-- hot100/json/
|   |-- hot100/markdown/
|   `-- skills/
|-- docs/
|   `-- agent-core-loop.md
|-- docker-compose.yml
`-- README.md
```

## Notes

Do not commit real API keys, database passwords, or production secrets. Local runtime files such as `.env` and data directories are ignored by `.gitignore`.
