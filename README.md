# AI Code Helper For Hot100

[中文文档](./README.zh-CN.md)

AI Code Helper For Hot100 is a Spring Boot and Vue based AI learning assistant for algorithm practice and backend interview preparation. The project focuses on the Hot100 practice workflow: problem browsing, progress tracking, wrong-answer analysis, personalized recommendations, study plans, streaming AI coaching, and a backend-controlled Agent Runtime.

The project is not positioned as a production SaaS platform. It is a practical backend project that demonstrates how to combine traditional business systems with LLM-driven Agent execution while keeping tool calls observable, permission-aware, and testable.

## Highlights

- User system with JWT authentication, refresh tokens, logout, and current-user APIs.
- Hot100 learning workflow with problem search, progress records, wrong book, weak-tag analysis, tag mastery, recommendations, and study plans.
- AI wrong-answer analysis that converts user code and error descriptions into structured feedback, weak knowledge points, and next actions.
- Streaming AI chat with role cards, solving modes, current-problem context, user learning profile, and optional MCP capability notice.
- Hand-built Agent Runtime with model/tool loop, backend tool registry, permission gate, runtime task slots, execution trace, recovery policy, hooks, long-term memory, skills, and sub-agent support.
- Explainable local RAG for Hot100 knowledge retrieval, based on markdown/json resources with source, slug, section, score, and matched terms.
- Backend engineering foundation with Spring Boot 3.5, Java 21, Spring Security, JPA, Flyway, MySQL, Redis fallback, RabbitMQ configuration, Docker Compose, and regression tests.

## Architecture

```text
Frontend (Vue 3)
  -> REST / SSE APIs
  -> Spring Boot Backend
       -> Auth / Chat / Hot100 / Agent Controllers
       -> Domain Services
       -> Agent Runtime
            -> AgentPromptBuilder
            -> Model turn
            -> tool_use JSON
            -> AgentPermissionGate
            -> AgentToolRegistry
            -> Java tool handler
            -> tool_result
            -> next model turn or final_answer
       -> JPA Repositories
  -> MySQL / Redis / RabbitMQ
```

The model can decide which registered tool to call, but actual execution stays inside the backend. Each tool has a name, description, permission level, and Java handler, so model reasoning remains flexible while system behavior remains controlled.

## Agent Runtime

The Agent module is built around a persistent task model:

```text
AgentTask
  taskId, userId, goal, status, finalAnswer
      |
      | 1:N
      v
RuntimeSlot
  runtimeId, attempt, executorId, status, stage, progress, heartbeatAt
      |
      | 1:N
      v
AgentStep
  runtimeId, stepOrder, toolName, input, output, status, latencyMs
```

This design separates a user goal from one or more execution attempts. It makes retries, runtime trace inspection, and future executor failover easier to support.

Current Agent capabilities include:

- ReAct-style loop: `messages -> model -> tool_use -> tool_result -> final_answer`
- Tool permissions: `READ`, `WRITE`, `EXTERNAL`, `SENSITIVE`
- Runtime trace persistence through `AgentStep`
- Invalid output, unknown tool, tool exception, and max-turn recovery
- Hook events for model turns, tool calls, permission denial, compaction, and recovery
- Long-term user memory for weaknesses, wrong-answer patterns, notes, and next actions
- Skill loading and focused sub-agent execution
- Runtime task slots with status, progress, heartbeat, and per-runtime step history
- Runtime heartbeat and watchdog checks for stale running slots
- SSE streaming for live Agent execution events: `model_turn`, `tool_result`, `tool_error`, `finish`, and `error`
- Optional MCP external tools registered into the Agent tool registry with `EXTERNAL` permission

## Hot100 Domain Features

- Browse and filter Hot100 problems by keyword, tag, and difficulty.
- View problem details, patterns, core ideas, complexity, pitfalls, and markdown notes.
- Save learning progress with notes, wrong reasons, weak knowledge points, AI feedback, and next actions.
- Maintain a wrong book and generate wrong-answer analysis.
- Compute weak tags and tag mastery from progress records.
- Generate recommendation lists and study plans.
- Run a Hot100 Agent task synchronously, submit it as a background task, or stream live execution events through SSE.
- Inspect model/tool execution traces by task and runtime slot.

## AI and RAG

`AgentKnowledgeService` provides the Agent's `retrieveKnowledge` tool.

The current retrieval baseline:

- loads `src/main/resources/hot100/markdown/*.md`
- loads `src/main/resources/hot100/json/*.json`
- splits markdown into section-level chunks
- enriches chunks with problem metadata such as slug, title, difficulty, tags, pattern, and summary
- returns explainable retrieval fields: source, slug, title, section, score, matched terms, and content
- keeps LangChain4j `ContentRetriever` as an optional extension point for vector retrieval

This gives the project a deterministic local RAG path for tests and demos, while leaving room for embedding-based search.

## MCP Integration

The project has optional MCP integration for external web-search capabilities. MCP is wired into both the normal streaming chat path and the Hot100 Agent tool registry.

- `McpConfig`: creates the MCP client and `McpToolProvider` when MCP is enabled.
- `AiCodeHelperServiceFactory`: attaches the MCP tool provider to the LangChain4j AI service.
- `QwenMcpCapabilityService`: injects an MCP capability notice into the chat prompt.
- `AiController`: passes the MCP capability notice into the SSE chat request.
- `Hot100AgentToolRegistry`: dynamically registers MCP tools as `mcp_*` Agent tools with `EXTERNAL` permission when an MCP client is available.

MCP is disabled by default. To enable DashScope WebSearch MCP, configure:

```env
DASHSCOPE_API_KEY=your_dashscope_api_key
APP_MCP_ENABLED=true
APP_MCP_SSE_URL=https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp
APP_MCP_WEB_SEARCH_TOOL_NAME=web_search
APP_MCP_WEB_SEARCH_QUERY_ARGUMENT=query
```

`APP_MCP_API_KEY` is optional because the backend falls back to `DASHSCOPE_API_KEY`:

```yaml
app.mcp.api-key: ${APP_MCP_API_KEY:${DASHSCOPE_API_KEY:}}
```

When using Docker Compose, the root `.env` file is loaded by Compose. When running locally with `.\mvnw.cmd spring-boot:run`, PowerShell does not automatically load `.env`; export the variables in the current shell first:

```powershell
$env:APP_MCP_ENABLED="true"
$env:APP_MCP_SSE_URL="https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp"
$env:APP_MCP_WEB_SEARCH_TOOL_NAME="web_search"
$env:APP_MCP_WEB_SEARCH_QUERY_ARGUMENT="query"
.\mvnw.cmd spring-boot:run
```

For Agent tasks, external MCP tools are permission-gated. A request must explicitly allow external tools, otherwise tools registered with `EXTERNAL` permission are denied by the backend permission gate.

## SSE Streaming

The project exposes two streaming paths:

- `GET /api/ai/chat`: streams normal AI chat tokens to the frontend through Server-Sent Events.
- `POST /api/agent/hot100/run/stream`: streams Hot100 Agent runtime events while the Agent loop is running.

Agent stream events are emitted as named SSE events. The event payload is serialized from `AgentStreamEvent`:

```json
{
  "type": "tool_result",
  "turn": 2,
  "toolName": "getWeakTags",
  "data": "...",
  "latencyMs": 123,
  "status": "SUCCESS"
}
```

Supported Agent event types:

- `model_turn`: the model completed one reasoning turn.
- `tool_result`: a tool call completed successfully.
- `tool_error`: a tool call failed.
- `finish`: the Agent produced the final answer.
- `error`: the Agent run failed.

## Main APIs

Auth:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`

Chat:

- `GET /api/ai/chat` - SSE streaming chat

Hot100:

- `GET /api/hot100/problems`
- `GET /api/hot100/problems/{slug}`
- `POST /api/hot100/progress`
- `GET /api/hot100/progress`
- `GET /api/hot100/weak-tags`
- `GET /api/hot100/tag-mastery`
- `GET /api/hot100/wrong-book`
- `POST /api/hot100/wrong-book/analyze`
- `GET /api/hot100/recommendations`
- `GET /api/hot100/study-plan`

Agent:

- `POST /api/agent/hot100/run`
- `POST /api/agent/hot100/run/stream` - SSE live Agent run
- `POST /api/agent/hot100/tasks`
- `GET /api/agent/hot100/tasks/{taskId}`
- `GET /api/agent/hot100/tasks/{taskId}/trace`
- `GET /api/agent/hot100/tasks/{taskId}/steps`
- `GET /api/agent/hot100/tasks/{taskId}/runtimes/{runtimeId}/steps`
- `GET /api/agent/memory`
- `GET /api/agent/memory/profile`
- `POST /api/agent/memory`

## Tech Stack

- Backend: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Bean Validation, Actuator
- AI: LangChain4j, DashScope/Qwen, SSE streaming, optional MCP integration
- Data: MySQL 8, Flyway, Redis cache with local fallback
- Frontend: Vue 3, Vite, Axios
- DevOps: Docker, Docker Compose, Maven Wrapper
- Testing: JUnit 5, Spring Boot Test, focused Agent and API smoke tests

## Quick Start

Create environment files:

```powershell
Copy-Item .env.example .env
Copy-Item ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

Configure at least:

- `DASHSCOPE_API_KEY`
- `APP_AUTH_JWT_SECRET`
- MySQL connection settings
- Redis and RabbitMQ settings if you use the full compose stack
- Optional MCP settings if WebSearch MCP is enabled: `APP_MCP_ENABLED`, `APP_MCP_SSE_URL`, `APP_MCP_WEB_SEARCH_TOOL_NAME`, `APP_MCP_WEB_SEARCH_QUERY_ARGUMENT`

Start infrastructure and application with Docker:

```bash
docker compose up -d --build
```

Run backend locally:

```powershell
.\mvnw.cmd spring-boot:run
```

Run frontend locally:

```powershell
cd ai-code-helper-frontend
npm install
npm run dev
```

## Verification

Run all backend tests:

```powershell
.\mvnw.cmd test
```

Run focused Agent tests:

```powershell
.\mvnw.cmd test "-Dtest=AgentMemoryServiceTest,AgentKnowledgeServiceTest,RuntimeTaskServiceTest,Hot100AgentServiceTest,AgentLoopServiceTest,AgentPromptBuilderTest"
```

Build frontend:

```powershell
cd ai-code-helper-frontend
npm run build
```

## Resume Summary

Built an AI learning assistant for Hot100 algorithm practice with Spring Boot and Vue. The backend implements JWT authentication, Hot100 learning progress, wrong-answer analysis, weak-tag analytics, recommendation and study-plan workflows, streaming AI chat, and a custom Agent Runtime.

Designed the Agent Runtime with a backend-controlled tool registry, permission-gated tool execution, runtime slots, persistent step traces, recovery policies, long-term memory, skill loading, and explainable local RAG retrieval. This keeps LLM reasoning flexible while keeping execution observable, testable, and controlled by backend services.

## Project Structure

```text
.
|-- ai-code-helper-frontend/
|-- src/main/java/com/yupi/aicodehelper/
|   |-- agent/
|   |   |-- core/
|   |   |-- Hot100AgentService.java
|   |   `-- Hot100AgentToolRegistry.java
|   |-- ai/
|   |-- auth/
|   |-- chat/
|   |-- controller/
|   |-- hot100/
|   |-- entity/
|   `-- repository/
|-- src/main/resources/
|   |-- db/migration/
|   |-- hot100/json/
|   |-- hot100/markdown/
|   |-- skills/
|   `-- system-prompt-role.txt
|-- docs/
|-- docker-compose.yml
`-- README.md
```

## Security Notes

Do not commit real API keys, database passwords, JWT secrets, or production credentials. Local `.env` files and runtime data directories should stay outside version control.
