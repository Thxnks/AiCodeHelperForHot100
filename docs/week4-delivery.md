# Week4 Delivery: Docker Compose Startup

## Prerequisites

- Docker Desktop 4.0+
- At least 4 GB memory available for containers

## Quick Start

1. Copy env templates:

```bash
cp .env.example .env
cp ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

2. Set required keys in `.env`:

- `DASHSCOPE_API_KEY`
- `APP_AUTH_JWT_SECRET`

3. Start full stack:

```bash
docker compose up -d --build
```

4. Verify endpoints:

- Frontend: `http://localhost:3001`
- Backend health: `http://localhost:8081/api/health`
- Swagger: `http://localhost:8081/api/swagger-ui.html`
- RabbitMQ management: `http://localhost:15672` (default `guest/guest`)

## Useful Commands

```bash
# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Restart backend only
docker compose restart backend

# Stop all containers
docker compose down

# Stop and clear DB/Redis data volumes
docker compose down -v
```

## Troubleshooting

- Backend fails on DB connection:
  check `docker compose logs mysql` and ensure `MYSQL_PASSWORD` in `.env` is not empty.
- Backend reports Redis unavailable:
  ensure Redis container is healthy, or set `APP_CACHE_REDIS_ENABLED=false`.
- Frontend cannot call backend:
  check `VITE_API_BASE_URL` used at frontend build time (compose build arg).
