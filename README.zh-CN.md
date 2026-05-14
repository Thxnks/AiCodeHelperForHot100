# AI Code Helper For Hot100

[English](./README.md)

AI Code Helper 是一个面向 Hot100 算法学习场景的 Spring Boot Agent Runtime 项目。它不是简单的大模型 API 包装，而是由后端控制工具调用、权限、后台运行槽位、执行轨迹和异常恢复，让模型通过结构化 `tool_use` JSON 决定下一步推理。

## 项目定位

这个项目适合用于展示后端工程能力和 AI Agent 应用开发能力：

- 后端基础：Spring Boot 3.5、Java 21、Spring Security、JWT、JPA、Flyway、Redis、RabbitMQ、Docker Compose。
- Agent Runtime：多轮循环、工具注册表、工具结果回灌、todo 状态、长期记忆、skills、sub-agent、上下文压缩、权限控制、hook、recovery、任务图、后台运行槽位。
- Hot100 业务场景：学习进度、薄弱标签、错因诊断、个性化推荐、学习计划、题目搜索、可解释 RAG 知识检索。

## Agent Runtime 架构

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

模型可以选择工具，但不能任意执行代码。每个工具都必须由后端注册，包含工具名、描述、权限级别和 Java handler。

## 任务运行模型

项目把“任务定义”和“运行尝试”拆开：

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

后台任务状态流转：

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

这样同一个任务后续可以支持多次执行、重试、executor failover，并且每次执行都有独立 trace。

## Agent 能力

- ReAct 风格循环：`messages -> model -> tool_use -> tool_result -> next model turn -> final_answer`
- 工具权限级别：`READ`、`WRITE`、`EXTERNAL`、`SENSITIVE`
- 内置工具：`todo_read`、`todo_write`、`list_skills`、`load_skill`、`task_create`、`task_update`、`task_get`、`task_list`
- Hot100 工具：进度查询、薄弱标签、标签掌握度、错题本、推荐、学习计划、题目详情、知识检索、受控进度更新
- 运行时可观测：`agent_task`、`latestRuntime`、`runtimeHistory`、`agent_step`
- 可解释本地 RAG：对 Hot100 markdown/json 做 chunk，带 `slug/title/section` 元数据、匹配词和分数，并通过 `retrieveKnowledge` 返回
- 长期记忆：把错因模式、薄弱知识点、笔记和下一步行动持久化为用户记忆，并通过 `memory_recall`、`memory_profile`、`memory_save` 暴露给 Agent
- recovery：非法模型输出、未知工具、工具异常、最大轮次终止
- hook：模型轮次、工具调用、权限拒绝、上下文压缩、recovery
- sub-agent：用于单题分析和错因复盘

## 核心接口

Hot100：

- `GET /api/hot100/problems`
- `GET /api/hot100/problems/{slug}`
- `POST /api/hot100/progress`
- `GET /api/hot100/tag-mastery`
- `GET /api/hot100/wrong-book/analysis`
- `POST /api/hot100/wrong-book/analyze`
- `GET /api/hot100/ai-recommendations`

Agent：

- `GET /api/agent/memory`
- `GET /api/agent/memory/profile`
- `POST /api/agent/memory`
- `POST /api/agent/hot100/run`
- `POST /api/agent/hot100/tasks`
- `GET /api/agent/hot100/tasks/{taskId}`
- `GET /api/agent/hot100/tasks/{taskId}/steps`
- `GET /api/agent/hot100/tasks/{taskId}/runtimes/{runtimeId}/steps`

Chat：

- `GET /api/ai/chat`

## RAG 知识检索

`AgentKnowledgeService` 为 Hot100 Agent 提供 `retrieveKnowledge` 工具。

当前基线能力：

- 加载 `hot100/markdown/*.md` 和 `hot100/json/*.json`
- 按 Markdown 标题切分 section 级 chunk
- 为 chunk 补充题目元数据：`slug`、`title`、`difficulty`、`tags`、`pattern`、`summary`
- 返回可解释检索字段：`source`、`slug`、`title`、`section`、`score`、`matchedTerms`、`content`
- 保留 LangChain4j `ContentRetriever` 作为后续接入向量库的扩展点

这让项目在没有外部向量库的情况下也有稳定可测的本地 RAG 基线，同时后续可以平滑升级到 embedding / vector search。

## Agent Memory

`AgentMemoryService` 为 Agent 提供长期用户记忆。

记忆记录包含：

- `type`：`USER_PREFERENCE`、`WEAKNESS`、`WRONG_ANSWER`、`NEXT_ACTION`、`NOTE`
- `scope`：通常为 `hot100`
- `subject`：题目 slug 或主题
- `content`：长期记忆内容
- `importance`：1-10
- `source`：`agent`、`progress` 或其它来源

Hot100 进度更新和错题分析流程会自动写入笔记、薄弱知识点、错因模式和下一步行动。Agent 可以通过 `memory_recall` 召回记忆，也可以通过 `memory_profile` 汇总用户画像。

也可以通过 REST API 查看或写入记忆：

- `GET /api/agent/memory?query=dp&limit=10`
- `GET /api/agent/memory/profile`
- `POST /api/agent/memory`

## 技术栈

- 后端：`Java 21`、`Spring Boot 3.5`、`Spring Security`、`Spring Data JPA`、`Flyway`
- AI：`LangChain4j`、`DashScope/Qwen`
- 前端：`Vue 3`、`Vite`、`Axios`、`SSE`
- 存储和中间件：`MySQL 8`、`Redis`、`RabbitMQ`
- 部署：`Docker`、`Docker Compose`

## 快速启动

```powershell
Copy-Item .env.example .env
Copy-Item ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

至少配置：

- `DASHSCOPE_API_KEY`
- `APP_AUTH_JWT_SECRET`
- MySQL / Redis / RabbitMQ 连接信息

Docker 启动：

```bash
docker compose up -d --build
```

本地后端：

```powershell
.\mvnw.cmd spring-boot:run
```

本地前端：

```powershell
cd ai-code-helper-frontend
npm install
npm run dev
```

## 验证

运行 Agent 核心回归测试：

```powershell
.\mvnw.cmd test "-Dtest=AgentMemoryServiceTest,AgentKnowledgeServiceTest,RuntimeTaskServiceTest,Hot100AgentServiceTest,AgentLoopServiceTest,AgentPromptBuilderTest"
```

运行全部后端测试：

```powershell
.\mvnw.cmd test
```

构建前端：

```powershell
cd ai-code-helper-frontend
npm run build
```

## 简历描述

设计并实现面向 Hot100 算法学习场景的 AI Agent Runtime。后端支持多轮 model/tool 循环、受控工具注册表、工具结果回灌、写工具权限控制、长期记忆、本地 skills、sub-agent、上下文压缩、hook 事件、结构化 recovery、持久任务图、后台运行槽位、按 runtime 隔离的执行轨迹和可解释 RAG 检索。

在 Agent Runtime 之上构建 Hot100 业务工具层，覆盖进度查询、薄弱标签分析、错因诊断、个性化推荐、学习计划生成、题目搜索、RAG 知识检索和受控进度更新。系统让模型推理保持灵活，同时保证执行过程可控、可观测、可测试、权限安全。

## 项目结构

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
`-- README.zh-CN.md
```

## 说明

不要提交真实 API Key、数据库密码或生产密钥。本地运行文件如 `.env` 和运行时数据目录已经通过 `.gitignore` 忽略。
