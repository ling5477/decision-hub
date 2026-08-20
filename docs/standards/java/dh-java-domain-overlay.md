# Decision Hub Java 领域 Overlay

本文件只补充 DH 领域约束，不改变业务逻辑、NQ/DH contract、Schema、Golden Case、状态机、Authority 或当前阶段。冲突时以 `docs/current/STATUS.md` / `WORK_ORDER.md`、冻结合同和更严格安全边界为准。

## 原子写入

- `DH-JAVA-ATOMIC-001`：Envelope、Event、Feedback 与受保护 decision 写入遵守既有原子性合同；禁止部分提交后吞异常或将失败降级为静默成功。
- `DH-JAVA-ATOMIC-002`：跨 Repository/JDBC 写必须沿用已冻结事务边界；工程规范不得通过新 fallback 或补写逻辑改变 commit/rollback 语义。

## Append-only 与 forward-only

- `DH-JAVA-APPEND-001`：append-only 数据禁止原地覆盖；已冻结事件、evidence、checksum 和历史 attempt 禁止回写。
- `DH-JAVA-APPEND-002`：修复使用 forward-only migration、补充事件或新 evidence；禁止重写历史来满足当前检查。

## 身份与幂等

- `DH-JAVA-IDENTITY-001`：`eventId`、`envelopeId`、`feedbackId`、`correlationId`、`requestId` 和 idempotency key 必须稳定，来源与生命周期明确。
- `DH-JAVA-IDEMPOTENCY-001`：重试不得生成破坏幂等的新身份；并发重复请求、lease、tombstone、commit-unknown 和过期语义服从既有合同。

## Schema 与消费者兼容

- `DH-JAVA-SCHEMA-001`：Schema 版本变更不得破坏既有消费者；禁止未经合同变更流程添加破坏性字段语义、收窄类型或更改默认值。
- `DH-JAVA-SCHEMA-002`：历史 migration 不修改；兼容修复必须 forward-only，并由真实 PostgreSQL/JDBC 证据验证。

## Fail-closed 与安全边界

- `DH-JAVA-FAIL-CLOSED-001`：输入不一致、证据不完整、校验失败、低置信度、tenant/source 不匹配和持久化失败必须 fail-closed；禁止隐式成功 fallback。
- `DH-JAVA-SECURITY-001`：HMAC、timestamp、nonce、source allowlist、payload size、tenant binding、replay protection、provider trust policy 和 audit trail 保持既有边界。
- `DH-JAVA-RUNTIME-001`：工程规范不得启用 real HTTP/provider、NQ runtime integration、Agent/LangGraph 或 LIVE，也不得把 mock/test wiring 暴露到生产 profile。

## 数据一致性

- `DH-JAVA-CONSISTENCY-001`：一次逻辑决策中的多次查询必须明确 snapshot/transaction 一致性；禁止组合可能撕裂的独立快照。
- `DH-JAVA-CONSISTENCY-002`：事务隔离、CAS、锁、幂等 lease、cleanup 和多实例可见性必须与业务不变量一致；store failure 禁止回退 in-memory。

## 确定性证据

- `DH-JAVA-DETERMINISM-001`：时间、序列、排序、checksum、fingerprint 和 evidence 生成必须确定性；通过 `Clock`、项目 `TimeProvider` 或已批准时间抽象控制。
- `DH-JAVA-EVIDENCE-001`：报告使用仓库相对路径，不包含凭证、绝对路径或敏感业务数据；未运行的 CI、PostgreSQL/Testcontainers 或外部验证不得写成 PASS。

## Java / Spring 平台能力约束

- `DH-JAVA-MODERN-001`：record、sealed hierarchy、pattern switch 等正式能力只在保持 identity、Schema、序列化和 consumer compatibility 时使用；禁止为现代化批量重构稳定模型。
- `DH-JAVA-VTHREAD-001`：virtual threads 当前未启用；任何后续候选必须先验证 transaction/security context、persistent guard、rate limit、连接池、背压和 shutdown。
- `DH-JAVA-SPRING-TX-001`：Spring transaction 必须保持 Envelope/Event/Feedback 和 evidence 写入原子性；self invocation、private method、async boundary 或 fallback 不得伪造保护。
- `DH-JAVA-SPRING-ASYNC-001`：异步任务不得使用 common pool、raw thread 或 unmanaged executor 绕过 tenant/source、idempotency、nonce/replay、audit 和 observability。
- `DH-JAVA-REPRODUCIBILITY-001`：snapshot、sequence、checksum、prompt/model version 与 evidence 生成必须确定性；平台升级不得重写冻结历史。
