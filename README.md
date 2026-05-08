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

## Main APIs

- `GET /api/hot100/problems`
- `GET /api/hot100/problems/{slug}`
- `POST /api/hot100/progress`
- `GET /api/hot100/tag-mastery`
- `GET /api/hot100/wrong-book/analysis`
- `POST /api/hot100/wrong-book/analyze`
- `GET /api/hot100/ai-recommendations`
- `GET /api/ai/chat`

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
