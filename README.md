# AI Code Helper For Hot100

[中文文档](./README.zh-CN.md)

An AI-powered algorithm practice coach built with `Spring Boot 3`, `Vue 3`, and `LangChain4j`. The project focuses on LeetCode Hot100 practice, user learning profiles, wrong-answer diagnosis, personalized recommendations, and streaming AI tutoring.

## Highlights

- Hot100 dataset with 100 Chinese LeetCode-style problems loaded from JSON and Markdown resources.
- User progress tracking with statuses such as not started, completed, wrong, and mastered.
- Tag mastery analytics based on practiced count, mastered count, wrong count, and mastery rate.
- Hybrid recommendation flow: rule-based candidate recall plus AI coach-style explanation.
- AI wrong-answer analysis with structured JSON output, repair retry, fallback degradation, persistence, and call logging.
- AI chat enriched with current problem context and user learning profile.
- Hot100 Agent workflow with model-driven planning, tool allow-list execution, persisted tool-call trace, knowledge retrieval fallback, and MCP web-search extension.
- Production-style backend stack: Spring Security, JWT, JPA, Flyway, Redis cache, async task support, Docker Compose.

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
  +-- AuthService
  +-- Hot100ProblemLoader
  +-- Hot100ProgressService
  +-- Hot100WrongAnalysisService
  +-- AiCodeHelperService
  |
  +-- MySQL
  +-- Redis
  +-- RabbitMQ
```

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

## Hot100 Agent Flow

The Hot100 Agent upgrades the project from prompt-based tutoring to a task-oriented workflow.

```text
user goal
  -> LLM planner returns JSON tool plan
  -> backend immediately returns taskId for polling
  -> backend validates tool names against an allow-list
  -> execute Hot100 tools: progress, weak tags, mastery, wrong book, recommendations, study plan
  -> optionally retrieve knowledge from docs / Hot100 notes with vector search or keyword fallback
  -> optionally call configured MCP web search for external/current references
  -> persist agent_task and agent_step trace records
  -> LLM writes final report from tool observations, with template fallback
  -> frontend polls task and step APIs to render live trace
```

If the model planner is unavailable or returns invalid JSON, the backend falls back to a deterministic rule-based plan so the workflow remains usable.

## MCP Extension

MCP is integrated as a controlled Agent tool instead of an unrestricted model capability. When `app.mcp.enabled=true`, a MCP SSE endpoint is configured, and DashScope/Qwen credentials are available, the Hot100 Agent can plan and execute `callMcpWebSearch`; the backend wraps the MCP client, records the selected tool name, arguments, result, and fallback message into `agent_step`, and the frontend renders the MCP trace in the Agent console.

The default MCP settings are environment-driven:

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

Designed and implemented an agentic Hot100 algorithm practice coach. The system uses an LLM planner to generate a JSON tool-call plan from the user's goal, validates tool names through a backend allow-list, and executes tools for progress lookup, weak-tag analysis, wrong-answer diagnosis, recommendation generation, study-plan creation, and knowledge retrieval. Agent tasks run asynchronously and persist `agent_task` / `agent_step` traces for frontend polling and observability; after tool execution, the LLM writes a final report from tool observations, while planner/report failures automatically fall back to deterministic rules to preserve system stability.

Extended the Agent with an MCP Client tool layer. External web-search capability is exposed through a controlled `callMcpWebSearch` tool, with enablement checks, credential-safe degradation, argument normalization, and frontend trace rendering, making the project closer to mainstream Agent architectures that combine planning, tool calling, RAG, MCP, and observability.

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

## Project Structure

```text
.
|-- ai-code-helper-frontend/
|-- src/main/java/
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
|   `-- *.txt
|-- docs/
|-- docker-compose.yml
`-- README.md
```

## Notes

Do not commit real API keys, database passwords, or production secrets. Local runtime files such as `.env` and data directories are ignored by `.gitignore`.
