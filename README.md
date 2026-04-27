# AI Code Helper For Hot100

A full-stack project built with `Spring Boot 3 + Vue 3 + LangChain4j`, focused on an AI coding assistant and Hot100 training workflow that is runnable and extensible.

## Core Features

- AI chat and coding assistance (LangChain4j + DashScope)
- User authentication (register / login / refresh token)
- Conversation and message persistence
- Hot100 dataset, progress tracking, and learning analytics APIs
- Redis cache, RabbitMQ async processing, Flyway DB migrations
- One-command Docker Compose deployment for the full stack

## Tech Stack

- Backend: `Java 21`, `Spring Boot 3.5.3`, `Spring Security`, `Spring Data JPA`, `Flyway`, `Redis`, `RabbitMQ`
- AI: `LangChain4j 1.1.0` + `DashScope`
- Frontend: `Vue 3`
- Database: `MySQL 8.0`
- Deployment: `Docker`, `Docker Compose`

## Quick Start (Recommended: Docker)

### 1) Prepare environment files

```bash
cp .env.example .env
cp ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

For Windows PowerShell:

```powershell
Copy-Item .env.example .env
Copy-Item ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

Minimum required values in `.env`:

- `DASHSCOPE_API_KEY`
- `APP_AUTH_JWT_SECRET` (use a strong random string with at least 32 characters)

### 2) Start all services

```bash
docker compose up -d --build
```

### 3) Access endpoints

- Frontend: `http://localhost:3001`
- Backend Health: `http://localhost:8081/api/health`
- Swagger UI: `http://localhost:8081/api/swagger-ui.html`
- RabbitMQ Console: `http://localhost:15672` (default `guest/guest`)

## Local Development (Without Docker)

### Backend

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd ai-code-helper-frontend
npm install
npm run dev
```

## Useful Commands

```bash
# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Restart backend only
docker compose restart backend

# Stop all containers
docker compose down

# Stop and remove data volumes (MySQL/Redis/RabbitMQ)
docker compose down -v
```

## Environment Variables

See `.env.example` for full backend variables:

- AI: `DASHSCOPE_API_KEY`, `BIGMODEL_API_KEY`
- MySQL: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`
- Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`
- RabbitMQ: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`
- Auth: `APP_AUTH_JWT_SECRET`, `APP_AUTH_ACCESS_TOKEN_EXPIRE_SECONDS`, `APP_AUTH_REFRESH_TOKEN_EXPIRE_SECONDS`
- Cache: `APP_CACHE_REDIS_ENABLED` and `APP_CACHE_*_TTL_SECONDS`

Frontend variables are in `ai-code-helper-frontend/.env.example`:

- `VITE_API_BASE_URL`

## Main API Entry Points

- `/api/health` health check
- `/api/swagger-ui.html` API docs
- Controller source path: `src/main/java/com/yupi/aicodehelper/controller`

## Project Structure

```text
.
├── ai-code-helper-frontend/      # Frontend app
├── src/main/java/                # Backend source code
├── src/main/resources/
│   ├── db/migration/             # Flyway SQL migrations
│   ├── hot100/                   # Hot100 dataset files
│   └── roles/                    # Role configs
├── docker-compose.yml
├── Dockerfile
└── docs/                         # Design and delivery documents
```

## Additional Docs

- [Docker Delivery Guide](docs/week4-delivery.md)
- [Database Engineering](docs/week2-db-engineering.md)
- [Migration Cutover Runbook](docs/week2-migration-cutover-runbook.md)
- [Async Task Design](docs/week3-async-tasks.md)
- [Cache Consistency](docs/week3-cache-consistency.md)
- [API Contract Examples](docs/api-contract-examples.md)

## Notes

Local sensitive/runtime files are ignored by `.gitignore` (for example `.env` and `data/`). Do not commit real API keys or secrets.
