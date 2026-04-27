# TODO ROADMAP

## Progress Summary (as of 2026-04-26)
- Week1 鉴权与权限完善: delivered
- Week2 数据库工程化 + API 规范收口: delivered
- Week3 缓存一致性 + 异步任务: delivered
- Week4 可观测性 + 测试 + 交付: in progress

## Week4 Plan (Continue Execution)

### Phase A - Observability (in progress)
Tasks:
- [x] 接入安全审计日志（`traceId + userId + path + status + costMs`）
- [x] 接入 `TraceId` 透传响应头
- [x] 接入 `Actuator + Prometheus` 指标端点
- [ ] 指标看板说明（关键指标与告警阈值建议）

Acceptance:
- 可通过 `/api/actuator/health` 进行健康检查。
- 管理员可访问 `/api/actuator/prometheus` 获取指标。

### Phase B - Test Hardening (in progress)
Tasks:
- [x] 修复 guardrail 行为测试断言（敏感词输入应抛异常）
- [ ] 拆分慢测试与快速回归测试分组（如 `unit` / `integration`）
- [ ] 增加鉴权链路 API 回归测试（登录/刷新/登出/403）

Acceptance:
- 默认回归测试在可控时间内完成。
- 关键鉴权接口具备可重复自动化校验。

### Phase C - Delivery Packaging (pending)
Tasks:
- [ ] 补齐后端 Dockerfile
- [ ] 补齐 `docker-compose.yml`（MySQL + Redis + backend + frontend）
- [ ] 输出启动文档与故障排查清单

Acceptance:
- 本地执行 `docker compose up` 可拉起完整链路。
- 文档可支持新环境一次启动成功。
