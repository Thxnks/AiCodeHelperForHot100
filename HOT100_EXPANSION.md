# Hot100 扩展到 100 题说明

当前项目已支持批量按 `resources/hot100/json/*.json` + `resources/hot100/markdown/*.md` 自动加载题库。

## 数据格式
- JSON 元数据字段：
  - `problemId`
  - `title`
  - `slug`
  - `leetCodeUrl`
  - `difficulty`
  - `tags`
  - `pattern`
  - `summary`
  - `coreIdea`
  - `pitfalls`
  - `complexity`
- Markdown 文件名规则：`{slug}.md`

## 扩展步骤
1. 向 `src/main/resources/hot100/json/` 补充题目 JSON。
2. 向 `src/main/resources/hot100/markdown/` 补充同名 Markdown。
3. 重启后端，`Hot100ProblemLoader` 会自动加载。
4. 访问 `GET /api/hot100/dataset-stats` 查看已加载数量是否达到 100。

## 校验建议
- `slug` 必须唯一。
- `leetCodeUrl` 应使用官方题目链接。
- `tags` 建议统一命名（如 `array`、`dynamic-programming`）。
