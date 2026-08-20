# Decision Hub Java Architecture Overlay

来源：root/module POM、`AGENTS.md`、current Authority、`dh-app` ArchitectureTest 与现有 contract/PostgreSQL tests。本文件描述实际架构，不决定 current stage。

## 模块方向

- `DH-ARCH-DOMAIN-001`：`dh-domain` 承载稳定领域 contract、value/state 语义，不依赖 API、provider、JDBC 或 Spring runtime 实现。
- `DH-ARCH-USECASE-001`：`dh-usecase` 承载应用编排、port 与 deterministic policy，不反向依赖具体 provider/JDBC adapter。
- `DH-ARCH-API-001`：`dh-api` 承载 HTTP boundary、DTO、认证前置与错误映射，不定义 persistence 主语义。
- `DH-ARCH-INFRA-001`：`dh-infra` 承载 JDBC、Flyway、Repository adapter 与事务协作，必须保持 store failure fail-closed。
- `DH-ARCH-APP-001`：`dh-app` 是 composition root，负责 profile、Bean wiring、managed Clock/executor 与 runtime guards。
- `DH-ARCH-PORT-001`：provider、connector、memory、knowledge、ledger、security、eval、scheduler 与 SDK 保持现有 port/adapter owner，不创建机械接口层。

## Spring 与并发边界

- `DH-ARCH-SPRING-001`：Spring stereotype、transaction、configuration 与 data repository 必须服从现有模块和 ArchUnit，不创建教科书式分层或 `ServiceImpl` ceremony。
- `DH-ARCH-EXECUTOR-001`：executor 变化必须审查 tenant/source 隔离、persistent guard、rate limit、MDC、连接池、背压与 shutdown；本任务不启用 virtual threads。
- `DH-ARCH-HTTP-001`：WebClient、RestTemplate 与 provider/connector 模型保持不变；不存在真实 target 时不得添加 retry/circuit breaker 或 real provider。

## 不变量保护

- `DH-ARCH-ATOMIC-001`：Java record、sealed hierarchy、pattern switch、async、transaction 或 virtual threads 不得弱化 Envelope/Event/Feedback atomicity、append-only、stable identity、idempotency、Schema compatibility 或 fail-closed。
- `DH-ARCH-EVIDENCE-001`：snapshot、checksum、sequence 与 evidence 必须确定性且不可回写；平台升级只增加治理 finding，不重写历史。
