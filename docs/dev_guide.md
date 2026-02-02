# 开发规范（骨架版）

- JDK 21
- 统一 ErrorCode + BizException + ApiResponse
- 所有对外 API 必须有 traceId
- DB：Flyway 管版本；禁止手工改表
- domain 不依赖 infra（用 ArchUnit 校验）
