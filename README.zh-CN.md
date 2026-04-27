# AI Code Helper For Hot100

[English](./README.md)

一个基于 `Spring Boot 3 + Vue 3 + LangChain4j` 的全栈项目，聚焦 AI 编程助手与 Hot100 训练流程，支持本地快速运行和持续扩展。

## 核心能力

- AI 对话与代码辅助（LangChain4j + DashScope）
- 用户认证（注册 / 登录 / 刷新 Token）
- 会话与消息持久化
- Hot100 题库、进度跟踪与学习数据接口
- Redis 缓存、RabbitMQ 异步处理、Flyway 数据库迁移
- Docker Compose 一键拉起完整环境

## 技术栈

- 后端：`Java 21`, `Spring Boot 3.5.3`, `Spring Security`, `Spring Data JPA`, `Flyway`, `Redis`, `RabbitMQ`
- AI：`LangChain4j 1.1.0` + `DashScope`
- 前端：`Vue 3`
- 数据库：`MySQL 8.0`
- 部署：`Docker`, `Docker Compose`

## 快速开始（推荐：Docker）

### 1）准备环境变量文件

```bash
cp .env.example .env
cp ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
Copy-Item ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

`.env` 至少需要配置：

- `DASHSCOPE_API_KEY`
- `APP_AUTH_JWT_SECRET`（建议 32 位以上随机字符串）

### 2）启动全部服务

```bash
docker compose up -d --build
```

### 3）访问地址

- 前端：`http://localhost:3001`
- 后端健康检查：`http://localhost:8081/api/health`
- Swagger UI：`http://localhost:8081/api/swagger-ui.html`
- RabbitMQ 管理台：`http://localhost:15672`（默认 `guest/guest`）

## 本地开发（不使用 Docker）

### 后端

```bash
./mvnw spring-boot:run
```

Windows：

```powershell
.\mvnw.cmd spring-boot:run
```

### 前端

```bash
cd ai-code-helper-frontend
npm install
npm run dev
```

## 常用命令

```bash
# 查看日志
docker compose logs -f backend
docker compose logs -f frontend

# 仅重启后端
docker compose restart backend

# 停止所有容器
docker compose down

# 停止并删除数据卷（MySQL/Redis/RabbitMQ）
docker compose down -v
```

## 环境变量

后端完整变量见 `.env.example`：

- AI：`DASHSCOPE_API_KEY`, `BIGMODEL_API_KEY`
- MySQL：`MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`
- Redis：`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`
- RabbitMQ：`RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`
- Auth：`APP_AUTH_JWT_SECRET`, `APP_AUTH_ACCESS_TOKEN_EXPIRE_SECONDS`, `APP_AUTH_REFRESH_TOKEN_EXPIRE_SECONDS`
- Cache：`APP_CACHE_REDIS_ENABLED` 和 `APP_CACHE_*_TTL_SECONDS`

前端变量见 `ai-code-helper-frontend/.env.example`：

- `VITE_API_BASE_URL`

## 主要接口入口

- `/api/health`：健康检查
- `/api/swagger-ui.html`：接口文档
- 控制器源码路径：`src/main/java/com/yupi/aicodehelper/controller`

## 项目结构

```text
.
|-- ai-code-helper-frontend/      # 前端项目
|-- src/main/java/                # 后端源码
|-- src/main/resources/
|   |-- db/migration/             # Flyway SQL 迁移
|   |-- hot100/                   # Hot100 数据文件
|   `-- roles/                    # 角色配置
|-- docker-compose.yml
|-- Dockerfile
`-- docs/                         # 设计与交付文档
```

## 相关文档

- [Docker 交付说明](docs/week4-delivery.md)
- [数据库工程设计](docs/week2-db-engineering.md)
- [迁移切换 Runbook](docs/week2-migration-cutover-runbook.md)
- [异步任务设计](docs/week3-async-tasks.md)
- [缓存一致性](docs/week3-cache-consistency.md)
- [API 合同示例](docs/api-contract-examples.md)

## 说明

本项目已通过 `.gitignore` 忽略本地敏感与运行时文件（如 `.env`、`data/`）。请勿提交真实密钥。
