# AI Code Helper For Hot100

一个面向算法刷题和面试准备的 AI 个性化学习系统。项目基于 `Spring Boot 3 + Vue 3 + LangChain4j`，围绕 LeetCode Hot100 题库构建用户刷题画像，支持进度记录、错题本、AI 错因分析、个性化推荐和流式 AI 辅导。

[English](./README.md)

## 项目亮点

- **Hot100 中文题库**：内置 100 道 Hot100 题目，使用 JSON 元数据和 Markdown 题解摘要加载。
- **学习进度跟踪**：支持未做、已做、做错、已掌握等状态，记录学习备注和复盘信息。
- **标签掌握度分析**：根据用户进度统计每个算法标签的练习数、掌握数、错误数和掌握率。
- **规则召回 + AI 解释推荐**：先用规则召回候选题，再结合用户画像生成推荐理由、训练重点和训练计划。
- **AI 错因分析**：用户提交错误代码或错误描述后，系统结合题目上下文调用大模型生成结构化错因分析。
- **AI 调用治理层**：大模型调用包含 prompt 构建、JSON 解析校验、JSON 修复重试、兜底降级、调用日志记录和结果落库。
- **个性化 AI 聊天**：AI 对话会注入当前题目上下文和用户学习画像，让回答更贴近用户薄弱点。
- **工程化能力**：集成 Spring Security、JWT、JPA、Flyway、Redis 缓存、异步任务和 Docker Compose。

## 技术栈

- 后端：`Java 21`, `Spring Boot 3.5`, `Spring Security`, `Spring Data JPA`, `Flyway`
- AI：`LangChain4j`, `DashScope/Qwen`
- 前端：`Vue 3`, `Vite`, `Axios`, `SSE`
- 数据库与中间件：`MySQL 8`, `Redis`, `RabbitMQ`
- 部署：`Docker`, `Docker Compose`

## 系统架构

```text
Vue Frontend
  |
  | REST / SSE
  v
Spring Boot API
  |
  +-- AuthService                  用户认证与 JWT
  +-- Hot100ProblemLoader          加载 Hot100 JSON/Markdown 题库
  +-- Hot100ProgressService        进度、错题本、标签掌握度、规则推荐
  +-- Hot100WrongAnalysisService   AI 错因分析治理层
  +-- AiCodeHelperService          LangChain4j 大模型服务
  |
  +-- MySQL                        用户、会话、进度、AI 调用日志
  +-- Redis                        热点数据缓存
  +-- RabbitMQ                     异步任务扩展
```

## 推荐系统流程

```text
用户刷题进度
  -> tagMastery 统计标签掌握度
  -> weakTags 找薄弱标签
  -> recommendNext 按薄弱标签召回候选题
  -> aiRecommendations 生成推荐理由、训练重点、教练总结
  -> 前端展示 AI 推荐卡片
```

核心设计不是让大模型直接从全部题库里随意推荐，而是采用：

```text
规则召回候选题 + 用户画像分析 + AI 教练式解释
```

这样推荐结果更稳定，也更容易解释和调试。

## AI 错因分析流程

```text
用户提交错误代码 / 错误描述 / 学习备注
  -> 后端根据 problemSlug 查询题目上下文
  -> 构造包含题目模式、核心思路、常见坑点的 prompt
  -> 调用大模型生成结构化 JSON
  -> 后端解析并校验 wrongReason / knowledgePoint / aiFeedback / nextAction
  -> JSON 失败时调用修复 prompt 重试
  -> 仍失败时使用保守兜底结果
  -> 保存到 hot100_problem_progress
  -> 写入 ai_call_log 记录耗时、成功状态、修复状态、兜底状态
```

这使大模型不是一次性文本生成工具，而是系统中的推理引擎；后端负责稳定性、可观测性和业务闭环。

## 核心接口

### Hot100 题库

- `GET /api/hot100/problems`：题目列表，支持关键词、标签、难度过滤
- `GET /api/hot100/problems/{slug}`：题目详情
- `GET /api/hot100/tags`：标签列表

### 用户进度与画像

- `POST /api/hot100/progress`：保存题目进度和错因字段
- `GET /api/hot100/progress`：查询用户进度
- `GET /api/hot100/tag-mastery`：查询标签掌握度
- `GET /api/hot100/weak-tags`：查询薄弱标签

### 错题与推荐

- `GET /api/hot100/wrong-book`：错题列表
- `GET /api/hot100/wrong-book/analysis`：错题分析列表
- `POST /api/hot100/wrong-book/analyze`：AI 分析错因
- `GET /api/hot100/recommendations`：规则推荐下一题
- `GET /api/hot100/ai-recommendations`：AI 教练推荐解释
- `GET /api/hot100/study-plan`：学习计划

### AI 聊天

- `GET /api/ai/chat`：SSE 流式聊天，支持角色、解题模式和当前题目上下文。

## 快速开始

### 1. 准备环境变量

```powershell
Copy-Item .env.example .env
Copy-Item ai-code-helper-frontend/.env.example ai-code-helper-frontend/.env
```

至少需要配置：

- `DASHSCOPE_API_KEY`
- `APP_AUTH_JWT_SECRET`
- MySQL / Redis / RabbitMQ 连接信息

### 2. Docker Compose 启动

```bash
docker compose up -d --build
```

访问：

- 前端：`http://localhost:3001`
- 后端健康检查：`http://localhost:8081/api/health`
- Swagger：`http://localhost:8081/api/swagger-ui.html`

### 3. 本地开发启动

后端：

```powershell
.\mvnw.cmd spring-boot:run
```

前端：

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

## 项目结构

```text
.
|-- ai-code-helper-frontend/       Vue 前端
|-- src/main/java/
|   |-- controller/                REST/SSE 接口
|   |-- hot100/                    Hot100 题库、进度、推荐、错因分析
|   |-- ai/                        LangChain4j AI 服务
|   |-- auth/                      认证与当前用户
|   |-- entity/                    JPA 实体
|   `-- repository/                JPA Repository
|-- src/main/resources/
|   |-- db/migration/              Flyway 迁移
|   |-- hot100/json/               题目元数据
|   |-- hot100/markdown/           题解摘要
|   `-- *.txt                      Prompt 模板
|-- docs/                          架构与面试材料
|-- docker-compose.yml
`-- README.zh-CN.md
```

## 简历表述建议

> 设计并实现 AI 算法刷题教练系统，基于 Hot100 题库、用户进度、错题记录和标签掌握度构建学习画像，实现规则召回 + AI 解释的个性化推荐流程；封装大模型错因分析服务，支持结构化 JSON 输出校验、失败修复、降级兜底、调用日志记录和错因落库。

## 说明

本项目已通过 `.gitignore` 忽略本地敏感文件，例如 `.env`、运行时数据目录等。请勿提交真实 API Key、数据库密码或生产密钥。
