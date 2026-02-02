# decision-hub (complete skeleton v1.3)

Java 21 + Spring Boot 3.5.x 的“多模型协同决策平台”后端完整骨架（模块化单体），已一次性补齐：
- A 必加：工程基线 / 可观测闭环 / API契约&错误模型 / 核心域骨架（Run/Gate/Ledger/Provider）
- B 强烈建议：数据层最小闭环（Postgres+Flyway+JPA）、长期记忆/RAG接口先行、成本计量/配额、配置系统
- C 按需：异步与可靠性预留、多租户安全预留、测试体系（ArchUnit+Testcontainers）、文档与Codex工作协议

## 1. 编译与运行
编译：
- `mvn -DskipTests clean package`

运行（自动构建依赖模块）：
- `mvn -pl dh-app -am spring-boot:run`

健康检查：
- `GET http://localhost:8080/_ping`
- `GET http://localhost:8080/actuator/health`

## 2. 可选：本地依赖（开发用）
- `docker compose -f ops/docker-compose.yml up -d`

## 3. 目录一览
- `contracts/`：OpenAPI + JSON Schema + 事件协议
- `golden_cases/`：回归用例骨架
- `docs/`：ADR/开发规范/Codex执行协议（agents.md）
