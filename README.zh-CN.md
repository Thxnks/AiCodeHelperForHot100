# AI Code Helper For Hot100

[English](./README.md)

AI Code Helper For Hot100 是一个基于 Spring Boot 和 Vue 的 AI 学习助手，面向 Hot100 算法刷题和后端面试准备场景。项目覆盖题库浏览、学习进度、错题分析、个性化推荐、学习计划、流式 AI 辅导，以及由后端控制执行的 Agent Runtime。

这个项目不是简单的大模型 API 包装，也不包装成成熟商业 SaaS。它的定位是一个有完整业务闭环和工程深度的后端实习项目，用来展示传统后端业务系统如何和 LLM Agent 能力结合，同时保证工具调用可观测、可控、可测试。

## 项目亮点

- 用户系统：支持 JWT 登录注册、刷新 token、登出、当前用户信息查询。
- Hot100 学习闭环：题目检索、学习进度、错题本、薄弱标签、标签掌握度、推荐题单、学习计划。
- AI 错题分析：结合用户代码和错误描述，生成结构化错因、薄弱知识点、AI 建议和下一步行动。
- 流式 AI 对话：支持角色卡、解题模式、当前题目上下文、用户学习画像和 MCP 能力提示。
- 手搓 Agent Runtime：支持 model/tool 循环、后端工具注册表、权限门控、三层上下文压缩、后台任务运行槽、执行 trace、异常恢复、hook、长期记忆、skills 和 sub-agent。
- 可解释本地 RAG：基于 Hot100 markdown/json 资源构建知识检索，返回来源、题目、章节、分数和命中词。
- 后端工程基础：Spring Boot 3.5、Java 21、Spring Security、JPA、Flyway、MySQL、Redis fallback、RabbitMQ 配置、Docker Compose 和回归测试。

## 整体架构

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

模型可以决定调用哪个已注册工具，但真正的执行权在后端。每个工具都有名称、描述、权限等级和 Java handler，因此模型负责推理，系统负责受控执行。

## Agent Runtime

Agent 模块围绕持久化任务模型设计：

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

这个设计把“用户目标”和“具体执行尝试”拆开，同一个任务后续可以支持重试、多执行器接管和按 runtime 查看独立 trace。

当前 Agent 能力包括：

- ReAct 风格循环：`messages -> model -> tool_use -> tool_result -> final_answer`
- 工具权限等级：`READ`、`WRITE`、`EXTERNAL`、`SENSITIVE`
- 通过 `AgentStep` 持久化模型输出、工具输入输出、状态和耗时
- 对非法模型输出、未知工具、工具异常、最大轮次停止做恢复处理
- hook 事件：模型轮次、工具调用、权限拒绝、上下文压缩、异常恢复
- 三层上下文压缩：先裁剪低价值历史，再压缩大体积工具输出，最后用模型生成结构化摘要
- 长期用户记忆：记录薄弱点、错因模式、笔记和下一步行动
- skill 加载和 focused sub-agent 执行
- 后台运行槽：记录状态、阶段、进度、心跳和每次运行的 step 历史
- runtime heartbeat 和 watchdog 检查，用于发现长时间无心跳的运行槽
- Agent 执行过程 SSE 流式输出：`model_turn`、`tool_result`、`tool_error`、`finish`、`error`
- 可选 MCP 外部工具会注册进 Agent 工具表，并使用 `EXTERNAL` 权限控制

## Hot100 业务能力

- 按关键词、标签、难度筛选 Hot100 题目。
- 查看题目详情、解题模式、核心思路、复杂度、常见错误和 markdown 笔记。
- 保存学习进度、笔记、错因、薄弱知识点、AI 反馈和下一步行动。
- 维护错题本并生成错题分析。
- 根据做题记录计算薄弱标签和标签掌握度。
- 生成推荐题单和学习计划。
- 支持同步运行 Hot100 Agent、提交后台任务，或通过 SSE 实时查看 Agent 执行事件。
- 按任务和 runtime slot 查看模型 / 工具调用 trace。

## AI 与 RAG

`AgentKnowledgeService` 为 Agent 提供 `retrieveKnowledge` 工具。

当前检索基线：

- 加载 `src/main/resources/hot100/markdown/*.md`
- 加载 `src/main/resources/hot100/json/*.json`
- 按 markdown 标题切分章节级 chunk
- 为 chunk 补充题目元数据：slug、title、difficulty、tags、pattern、summary
- 返回可解释字段：source、slug、title、section、score、matchedTerms、content
- 保留 LangChain4j `ContentRetriever` 作为后续接入向量检索的扩展点

这样项目在没有外部向量库时也有稳定可测的本地 RAG 能力，后续也可以平滑升级到 embedding / vector search。

## MCP 集成

项目已经预留并接入了可选 MCP 能力，用于外部 WebSearch 等工具调用。当前 MCP 同时接入普通流式聊天和 Hot100 Agent 工具注册表。

- `McpConfig`：在 MCP 开启时创建 MCP client 和 `McpToolProvider`。
- `AiCodeHelperServiceFactory`：把 MCP tool provider 挂到 LangChain4j AI Service 上。
- `QwenMcpCapabilityService`：根据配置生成 MCP 能力提示。
- `AiController`：把 MCP 能力提示注入 SSE 流式聊天请求。
- `Hot100AgentToolRegistry`：当 MCP client 可用时，将 MCP 工具动态注册为 `mcp_*` Agent 工具，并标记为 `EXTERNAL` 权限。

MCP 默认关闭。如需开启 DashScope WebSearch MCP，在环境变量或 `.env` 中配置：

```env
DASHSCOPE_API_KEY=your_dashscope_api_key
APP_MCP_ENABLED=true
APP_MCP_SSE_URL=https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp
APP_MCP_WEB_SEARCH_TOOL_NAME=web_search
APP_MCP_WEB_SEARCH_QUERY_ARGUMENT=query
```

`APP_MCP_API_KEY` 可以不单独配置，因为后端会自动 fallback 到 `DASHSCOPE_API_KEY`：

```yaml
app.mcp.api-key: ${APP_MCP_API_KEY:${DASHSCOPE_API_KEY:}}
```

如果使用 Docker Compose 启动，根目录 `.env` 会被 Compose 读取。如果本地直接执行 `.\mvnw.cmd spring-boot:run`，PowerShell 不会自动读取 `.env`，需要先在当前 shell 中设置：

```powershell
$env:APP_MCP_ENABLED="true"
$env:APP_MCP_SSE_URL="https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp"
$env:APP_MCP_WEB_SEARCH_TOOL_NAME="web_search"
$env:APP_MCP_WEB_SEARCH_QUERY_ARGUMENT="query"
.\mvnw.cmd spring-boot:run
```

Agent 任务中的 MCP 外部工具受权限门控控制。请求需要显式允许 external 工具，否则 `EXTERNAL` 权限工具会被后端权限门拒绝执行。

## SSE 流式输出

项目目前有两条 SSE 流式链路：

- `GET /api/ai/chat`：普通 AI 聊天流式输出，前端通过 EventSource 接收。
- `POST /api/agent/hot100/run/stream`：Hot100 Agent 运行时事件流，Agent loop 执行过程中实时推送。

Agent 流式事件使用命名 SSE event，payload 来自 `AgentStreamEvent`：

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

当前 Agent 事件类型：

- `model_turn`：模型完成一轮推理。
- `tool_result`：工具调用成功。
- `tool_error`：工具调用失败。
- `finish`：Agent 生成最终答案。
- `error`：Agent 运行失败。

## 上下文压缩

Agent Runtime 使用三层上下文压缩策略，在长轮次任务中控制 prompt 长度，同时尽量保留关键执行状态：

1. `TIER1_SNIP`：对历史消息打分，裁剪低价值旧消息，同时保留原始用户目标和最近轮次。
2. `TIER2_MICROCOMPACT`：压缩大体积工具输出，尤其是 JSON object 和 array。
3. `TIER3_AUTOCOMPACT`：调用模型生成结构化摘要，保留目标、已完成工作、关键发现、剩余任务和 todo 状态。

压缩摘要会记录压缩层级和已完成轮次，`AgentPromptBuilder` 会把这些元数据重新注入后续模型轮次。

## 核心接口

认证：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`

聊天：

- `GET /api/ai/chat` - SSE 流式聊天

Hot100：

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

Agent：

- `POST /api/agent/hot100/run`
- `POST /api/agent/hot100/run/stream` - Agent SSE 实时运行
- `POST /api/agent/hot100/tasks`
- `GET /api/agent/hot100/tasks/{taskId}`
- `GET /api/agent/hot100/tasks/{taskId}/trace`
- `GET /api/agent/hot100/tasks/{taskId}/steps`
- `GET /api/agent/hot100/tasks/{taskId}/runtimes/{runtimeId}/steps`
- `GET /api/agent/memory`
- `GET /api/agent/memory/profile`
- `POST /api/agent/memory`

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Spring Security、Spring Data JPA、Bean Validation、Actuator
- AI：LangChain4j、DashScope/Qwen、SSE 流式输出、可选 MCP 集成
- 数据：MySQL 8、Flyway、Redis 缓存和本地 fallback
- 前端：Vue 3、Vite、Axios
- 工程化：Docker、Docker Compose、Maven Wrapper
- 测试：JUnit 5、Spring Boot Test、Agent 核心测试和 API smoke test

## 快速启动

创建环境文件：

```powershell
Copy-Item .env.example .env
Copy-Item ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

至少配置：

- `DASHSCOPE_API_KEY`
- `APP_AUTH_JWT_SECRET`
- MySQL 连接配置
- 如果使用完整 compose 栈，需要配置 Redis 和 RabbitMQ
- 如果开启 WebSearch MCP，需要配置 `APP_MCP_ENABLED`、`APP_MCP_SSE_URL`、`APP_MCP_WEB_SEARCH_TOOL_NAME`、`APP_MCP_WEB_SEARCH_QUERY_ARGUMENT`

使用 Docker 启动：

```bash
docker compose up -d --build
```

本地启动后端：

```powershell
.\mvnw.cmd spring-boot:run
```

本地启动前端：

```powershell
cd ai-code-helper-frontend
npm install
npm run dev
```

## 验证

运行全部后端测试：

```powershell
.\mvnw.cmd test
```

运行 Agent 核心测试：

```powershell
.\mvnw.cmd test "-Dtest=AgentMemoryServiceTest,AgentKnowledgeServiceTest,RuntimeTaskServiceTest,Hot100AgentServiceTest,AgentLoopServiceTest,AgentPromptBuilderTest"
```

构建前端：

```powershell
cd ai-code-helper-frontend
npm run build
```

## 简历描述

基于 Spring Boot 和 Vue 实现面向 Hot100 算法学习的 AI 学习助手。后端支持 JWT 用户认证、学习进度管理、错题分析、薄弱标签统计、推荐题单、学习计划、流式 AI 对话，以及自研 Agent Runtime。

设计并实现 Agent Runtime：包括后端受控工具注册表、权限门控工具执行、三层上下文压缩、后台运行槽、持久化 step trace、SSE 运行事件、异常恢复策略、长期记忆、skill 加载、MCP 外部工具和可解释本地 RAG。系统让大模型负责推理决策，同时由后端控制真实执行过程，提升可观测性、可测试性和执行安全性。

## 项目结构

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
`-- README.zh-CN.md
```

## 安全说明

不要提交真实 API Key、数据库密码、JWT 密钥或生产环境凭据。本地 `.env` 文件和运行时数据目录应保持在版本控制之外。
