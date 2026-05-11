# AI Code Helper For Hot100

[中文文档](./README.zh-CN.md)

An AI-powered Hot100 algorithm practice system built with `Spring Boot 3`, `Vue 3`, and `LangChain4j`. The project combines LeetCode-style practice data, user progress analytics, wrong-answer diagnosis, personalized recommendations, and an agentic Hot100 workflow.

## Technical Highlights

- Agent core loop instead of one-shot prompt calls: `messages -> model -> tool_use -> tool_result -> next model turn -> final_answer`.
- Backend tool registry with explicit tool names, descriptions, permission levels, and Java handlers.
- Hot100 Agent tools for progress lookup, weak-tag analysis, tag mastery, wrong-book inspection, recommendations, study-plan generation, problem search, knowledge retrieval, and focused sub-agent review.
- Built-in Agent state tools: `todo_read` and `todo_write` let the model maintain a short in-session plan during multi-step work.
- Permission gate for tool execution: read tools are allowed by default, while write tools such as `updateProgress` and `analyzeWrongAnswer` require `allowWrite=true`.
- Lightweight sub-agent support for focused problem analysis and wrong-answer review, with isolated child context and compact parent result.
- Local skill loading through `list_skills` and `load_skill`; current skills cover Hot100 review, wrong-answer analysis, and interview coaching.
- Context compaction for long current-run message history, preserving the original user goal, todos, turn count, and recent observations.
- Prompt pipeline extracted into `AgentPromptBuilder`, making model input assembly testable separately from tool dispatch.
- Recovery policy for invalid model JSON, unknown tools, tool exceptions, and max-turn stops; recovery is returned to the model as structured context when possible.
- Synchronous hook events for model turns, tool calls, permission denial, compaction, and recovery, without letting hook failures break the main loop.
- Persisted task trace: `agent_task` stores task status and final answer, while `agent_step` records model turns and tool execution details.
- Production-style backend foundation: Spring Security, JWT auth, JPA, Flyway migrations, Redis cache fallback, async task executor, Docker Compose.

## Tech Stack

- Backend: `Java 21`, `Spring Boot 3.5`, `Spring Security`, `Spring Data JPA`, `Flyway`
- AI: `LangChain4j`, `DashScope/Qwen`
- Frontend: `Vue 3`, `Vite`, `Axios`, `SSE`
- Storage and middleware: `MySQL 8`, `Redis`, `RabbitMQ`
- Deployment: `Docker`, `Docker Compose`

## Architecture

```text
Vue Frontend
  |
  | REST / SSE
  v
Spring Boot API
  |
  +-- Auth / JWT
  +-- Hot100ProblemLoader
  +-- Hot100ProgressService
  +-- Hot100WrongAnalysisService
  +-- AiCodeHelperService
  +-- Hot100AgentService
  |     |
  |     +-- AgentLoopService
  |     +-- AgentToolRegistry
  |     +-- AgentPromptBuilder
  |     +-- AgentPermissionGate
  |     +-- AgentRecoveryPolicy
  |     +-- AgentHookManager
  |
  +-- MySQL
  +-- Redis
  +-- RabbitMQ
```

## Hot100 Agent Flow

The Hot100 Agent is model-driven, but tool execution stays controlled by the backend.

```text
user goal
  -> create agent_task
  -> build Hot100 tool registry
  -> model returns one JSON object
  -> backend parses tool_use or final_answer
  -> permission gate checks requested tool
  -> backend executes registered Java handler
  -> append structured tool_result into messages
  -> next model turn observes real tool output
  -> persist model/tool trace into agent_step
  -> final_answer saved into agent_task
```

Supported Hot100 Agent business tools include:

- `getUserProgress`
- `getWeakTags`
- `getTagMastery`
- `getProblemDetail`
- `updateProgress`
- `analyzeWrongAnswer`
- `getWrongBook`
- `recommendNext`
- `aiRecommendations`
- `generateStudyPlan`
- `searchProblems`
- `retrieveKnowledge`
- `analyzeProblemWithSubAgent`
- `reviewWrongAnswerWithSubAgent`

Core Agent tools are registered automatically:

- `todo_read`
- `todo_write`
- `list_skills`
- `load_skill`

## Agent Reliability Design

The Agent loop includes several safeguards that are useful in backend interviews:

- Tool allow-list: the model can only call tools registered by the backend.
- Permission separation: read tools and write tools are modeled separately through `AgentToolPermissionLevel`.
- Structured recovery: invalid model output, missing tools, tool exceptions, and max-turn exits are handled by `AgentRecoveryPolicy`.
- Trace persistence: each model turn and tool execution is stored for auditability and frontend display.
- Context control: long message history is compacted before it grows without bound.
- Test coverage: core loop, prompt builder, permission behavior, hooks, recovery, sub-agent isolation, and Hot100 service trace persistence are covered by unit tests.

## Core AI Flow

Wrong-answer analysis is not implemented as a plain controller-to-model call.

```text
user code / error description
  -> problem context enrichment
  -> LLM structured JSON generation
  -> backend JSON parsing and validation
  -> one repair retry if parsing fails
  -> conservative fallback if the model is unavailable
  -> persist analysis into progress records
  -> write ai_call_log for latency, success, repair, fallback, and error tracking
```

## MCP Extension

The project contains an optional MCP integration for Qwen/DashScope-backed AI chat. When `app.mcp.enabled=true`, MCP SSE URL and API key are configured, a `McpClient` and `McpToolProvider` can be registered for LangChain4j.

Current Hot100 Agent core loop intentionally keeps MCP out of the first-stage Agent scope. Agent-side extension points are already present through the tool registry, permission levels, and hook system.

Default MCP settings are environment-driven:

- `APP_MCP_ENABLED=false`
- `APP_MCP_SSE_URL=`
- `APP_MCP_API_KEY=` defaults to `DASHSCOPE_API_KEY` when left empty
- `APP_MCP_WEB_SEARCH_TOOL_NAME=web_search`
- `APP_MCP_WEB_SEARCH_QUERY_ARGUMENT=query`

## Main APIs

- `GET /api/hot100/problems`
- `GET /api/hot100/problems/{slug}`
- `POST /api/hot100/progress`
- `GET /api/hot100/tag-mastery`
- `GET /api/hot100/wrong-book/analysis`
- `POST /api/hot100/wrong-book/analyze`
- `GET /api/hot100/ai-recommendations`
- `GET /api/ai/chat`
- `POST /api/agent/hot100/tasks`
- `POST /api/agent/hot100/run`
- `GET /api/agent/hot100/tasks/{taskId}`
- `GET /api/agent/hot100/tasks/{taskId}/steps`

## Resume Description

Designed and implemented an agentic Hot100 algorithm practice coach. The backend contains a reusable Agent core loop that lets an LLM call registered Java tools through structured `tool_use` JSON, feeds `tool_result` observations back into later model turns, and persists model/tool traces for auditability. The Agent supports in-session todos, local skill loading, focused sub-agents, context compaction, permission-gated write tools, hook events, prompt pipeline separation, and structured recovery for invalid model output and tool failures.

Built the Hot100 business tool layer on top of the Agent core, covering progress lookup, weak-tag analytics, tag mastery, wrong-book review, recommendation generation, study-plan generation, knowledge retrieval, and guarded progress updates. The result is closer to a real backend Agent system than a simple prompt wrapper: model reasoning is flexible, but execution remains controlled, observable, testable, and permission-aware.

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

```powershell
.\mvnw.cmd test

cd ai-code-helper-frontend
npm run build
```

Current backend test coverage includes Agent loop, prompt builder, Hot100 Agent service, application context, API contract smoke tests, and auth flow tests.

## Project Structure

```text
.
|-- ai-code-helper-frontend/
|-- src/main/java/
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
|   |-- skills/
|   `-- *.txt
|-- docs/
|   `-- agent-core-loop.md
|-- docker-compose.yml
`-- README.md
```

## Notes

Do not commit real API keys, database passwords, or production secrets. Local runtime files such as `.env` and data directories are ignored by `.gitignore`.
