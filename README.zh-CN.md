# AI Code Helper For Hot100

[English](./README.md)

一个面向 Hot100 算法刷题和后端 Agent 架构实践的 AI 学习系统。项目基于 `Spring Boot 3 + Vue 3 + LangChain4j`，围绕 LeetCode Hot100 题库构建用户学习画像、错题诊断、个性化推荐和可观测的 Hot100 Agent 工作流。

## 技术亮点

- **Agent 核心闭环**：不再是一次性 prompt 调用，而是 `messages -> model -> tool_use -> tool_result -> 下一轮 model -> final_answer` 的可循环执行模型。
- **后端工具注册表**：通过 `AgentToolRegistry` 管理工具名称、描述、权限级别和 Java Handler，模型只能调用后端注册过的工具。
- **Hot100 业务工具层**：支持进度查询、薄弱标签分析、标签掌握度、错题本、个性化推荐、学习计划、题目搜索、知识检索和子 Agent 分析。
- **内置 Todo 状态工具**：`todo_read` / `todo_write` 让模型在一次任务中维护短期计划，适合多步推理任务。
- **工具权限隔离**：读工具默认允许，`updateProgress`、`analyzeWrongAnswer` 等写工具必须显式传入 `allowWrite=true`。
- **轻量 Sub-Agent**：支持针对单题分析、错因复盘启动隔离上下文的子 Agent，父 Agent 只接收摘要和轮次数。
- **本地 Skill 机制**：通过 `list_skills` / `load_skill` 按需加载本地 Markdown 技能，目前包含 Hot100 复盘、错因分析、面试教练。
- **上下文压缩**：长会话会触发当前运行内 compaction，保留原始目标、todo、轮次数和最近观察，避免消息无限膨胀。
- **Prompt Pipeline**：将 prompt 拼装独立到 `AgentPromptBuilder`，让模型输入组装可以单独测试和演进。
- **Recovery 策略**：针对模型输出非法 JSON、未知工具、工具异常、超过最大轮次做统一恢复处理。
- **Hook 事件机制**：模型轮次、工具调用、权限拒绝、上下文压缩、恢复动作都会发布同步 hook，hook 异常不会破坏主流程。
- **任务轨迹持久化**：`agent_task` 记录任务状态和最终答案，`agent_step` 记录每次模型调用和工具执行细节。
- **后端工程化基础**：集成 Spring Security、JWT、JPA、Flyway、Redis 缓存降级、异步任务执行器和 Docker Compose。

## 技术栈

- 后端：`Java 21`、`Spring Boot 3.5`、`Spring Security`、`Spring Data JPA`、`Flyway`
- AI：`LangChain4j`、`DashScope/Qwen`
- 前端：`Vue 3`、`Vite`、`Axios`、`SSE`
- 数据库与中间件：`MySQL 8`、`Redis`、`RabbitMQ`
- 部署：`Docker`、`Docker Compose`

## 系统架构

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

## Hot100 Agent 流程

Hot100 Agent 由模型决定下一步，但所有工具执行都由后端受控完成。

```text
用户目标
  -> 创建 agent_task
  -> 构建 Hot100 工具注册表
  -> 模型返回一个 JSON 对象
  -> 后端解析 tool_use 或 final_answer
  -> 权限网关检查工具权限
  -> 后端执行注册过的 Java Handler
  -> 将结构化 tool_result 追加回 messages
  -> 下一轮模型观察真实工具结果
  -> 持久化 model/tool trace 到 agent_step
  -> final_answer 写回 agent_task
```

当前 Hot100 Agent 业务工具包括：

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

核心 Agent 工具会自动注册：

- `todo_read`
- `todo_write`
- `list_skills`
- `load_skill`

## Agent 可靠性设计

这部分是项目最适合面试展开的后端亮点：

- **工具白名单**：模型不能任意执行代码或调用不存在的工具，只能调用注册表中的工具。
- **权限分层**：通过 `AgentToolPermissionLevel` 区分 `READ`、`WRITE`、`EXTERNAL`、`SENSITIVE`。
- **结构化恢复**：非法模型输出、未知工具、工具异常和最大轮次终止都由 `AgentRecoveryPolicy` 统一处理。
- **可观测轨迹**：每次模型调用和工具执行都会落库，前端可以展示任务执行过程。
- **上下文控制**：长上下文触发压缩，避免 Agent 循环越来越重。
- **测试覆盖**：核心 loop、prompt builder、权限、hook、recovery、sub-agent 隔离、Hot100 trace 持久化都有单元测试覆盖。

## AI 错因分析流程

错因分析不是简单的 controller 直接调大模型，而是带有解析、修复、降级和落库的稳定流程。

```text
用户代码 / 错误描述
  -> 查询题目上下文
  -> 构造包含题目模式、核心思路、常见坑点的 prompt
  -> 调用大模型生成结构化 JSON
  -> 后端解析并校验 wrongReason / knowledgePoint / aiFeedback / nextAction
  -> JSON 失败时调用修复 prompt 重试
  -> 仍失败时使用保守兜底结果
  -> 保存到 hot100_problem_progress
  -> 写入 ai_call_log 记录耗时、成功状态、修复状态、兜底状态
```

## MCP 扩展

项目保留了 Qwen/DashScope AI Chat 侧的可选 MCP 集成。当 `app.mcp.enabled=true` 且配置了 MCP SSE URL 和 API Key 时，可以为 LangChain4j 注册 `McpClient` 和 `McpToolProvider`。

当前 Hot100 Agent 核心闭环阶段刻意没有把 MCP 放进 Agent 工具层，Agent 侧先聚焦 tool loop、权限、hook、prompt pipeline 和 recovery。后续如果要扩展外部工具，可以直接通过工具注册表和权限级别接入。

默认 MCP 配置：

- `APP_MCP_ENABLED=false`
- `APP_MCP_SSE_URL=`
- `APP_MCP_API_KEY=` 留空时默认读取 `DASHSCOPE_API_KEY`
- `APP_MCP_WEB_SEARCH_TOOL_NAME=web_search`
- `APP_MCP_WEB_SEARCH_QUERY_ARGUMENT=query`

## 核心接口

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

## 简历描述建议

设计并实现一个 Agentic Hot100 算法刷题教练系统。后端抽象了可复用的 Agent 核心闭环，支持大模型通过结构化 `tool_use` JSON 调用注册过的 Java 工具，并将 `tool_result` 回写到消息上下文供下一轮模型决策；系统持久化模型调用和工具执行轨迹，具备可审计和可观测能力。Agent 支持 in-session todo、本地 skill 加载、轻量子 Agent、上下文压缩、写工具权限控制、hook 事件、prompt pipeline 拆分和非法模型输出/工具异常的结构化恢复。

在 Agent 核心之上构建 Hot100 业务工具层，覆盖用户进度查询、薄弱标签分析、标签掌握度、错题本复盘、个性化推荐、学习计划生成、知识检索和受控进度更新。项目不是简单的大模型问答包装，而是把模型推理和后端受控执行结合起来，强调可控、可观测、可测试和权限安全。

## 快速开始

```powershell
Copy-Item .env.example .env
Copy-Item ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

至少需要配置：

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

## 本地验证

```powershell
.\mvnw.cmd test

cd ai-code-helper-frontend
npm run build
```

当前后端测试覆盖 Agent loop、prompt builder、Hot100 Agent service、应用上下文、API contract smoke test 和认证流程测试。

## 项目结构

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
`-- README.zh-CN.md
```

## 说明

请不要提交真实 API Key、数据库密码或生产密钥。本地运行文件如 `.env` 和运行时数据目录已通过 `.gitignore` 忽略。
