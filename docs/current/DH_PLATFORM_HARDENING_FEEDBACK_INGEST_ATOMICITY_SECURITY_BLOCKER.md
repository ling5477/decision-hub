# DH Platform Hardening Feedback Ingest Atomicity Security Blocker

## 1. Authority and decision

```text
Task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-SECURITY-BLOCKER
Primary classification: SECURITY_AUDIT
Task result: FIXED / LOCAL_SECURITY_REMEDIATED / NOT PUSHED
Remediation baseline: 2cb44f406ff618a531d1ca44e2c1e6848750ed75
Origin baseline: 16ecded2f3708d69e05afe5f2a4f621c823d8be3
Final close: BLOCKED ATTEMPT PRESERVED / RETRY PENDING
P1-1 duplicate validation order: FIXED
P1-2 exact event correlation: FIXED
Active P0 / P1: 0 / 0
Implementation publication: NOT_STARTED
Close tag: ABSENT
Production capacity: NOT_PROVEN
Next task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-MILESTONE-FINAL-CLOSE-RETRY
```

本任务只修复 milestone final-close exact-diff security review 阻断的两条 P1，并完成累计 diff 的重新安全
复核。原 final-close 的 `BLOCKED` 与 `SECURITY_EXACT_DIFF_REVIEW: FAIL` 是不可改写的历史事实；本任务没有
执行 final close、push、tag、archive、capacity gate 或任何真实外部集成。

## 2. Scope and boundaries

### Production files changed

- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/NqFeedbackEventRepository.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/impl/DefaultNqFeedbackIngestionService.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/inmemory/InMemoryNqFeedbackEventRepository.java`
- `dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNqFeedbackEventRepository.java`

### Test files changed

- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIdempotencyTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIngestionAtomicityTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/inmemory/BoundedInMemoryNqFeedbackEventRepositoryTest.java`
- `dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNqFeedbackEventRepositoryTest.java`
- `dh-api/src/test/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackControllerWebMvcTest.java`
- `dh-app/src/test/java/com/guidinglight/decisionhub/FeedbackIngestAtomicityFlywayPostgresTest.java`

### Factsources changed

- `README.md`
- `docs/current/README.md`
- `docs/current/STATUS.md`
- `docs/current/WORK_ORDER.md`
- `docs/current/ROADMAP.md`
- `docs/current/TESTING.md`
- `docs/current/WORKLOG.md`
- `docs/current/CODEX_PROJECT_INSTRUCTIONS.md`
- `docs/current/DH_PLATFORM_HARDENING_FEEDBACK_INGEST_ATOMICITY_IMPLEMENTATION_WORK_ORDER.md`
- 本文件。

```text
VALIDATION_SCOPE subset READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE subset WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE subset FACTSOURCE_WRITE_ALLOWLIST: PASS
SCOPE INVARIANTS: PASS / 3 OF 3
UNEXPECTED FILES: 0
```

明确未修改：API main、DTO、OpenAPI、migration、schema、contracts、golden cases、POM、workflow、其他业务
Repository、NQ、HTTP/provider、Agent、LangGraph、learning stores、capacity 与 `docs/gates`。

## 3. P1 remediation

### 3.1 Validation and canonicalization precede duplicate resolution

最终执行顺序固定为：

```text
parse
-> structural validation
-> tenant/source/event-type validation
-> strict canonicalization
-> canonical payload/fingerprint calculation
-> atomic unit of work
-> duplicate/conflict/completion resolution
```

- 已存在 `eventId` 不再绕过当前请求校验。
- 缺少 required field、非法 event type 或重复 JSON object key 均在进入 unit of work 前按既有错误语义拒绝。
- 重复 object key 使用 Jackson `StreamReadFeature.STRICT_DUPLICATE_DETECTION`，返回既有
  `INVALID_SCHEMA`，不新增 wire error code，不写 envelope/event。
- JSON object key 重排仍按 canonical JSON 语义判为相同；数组顺序、字段值、数值或类型变化仍产生冲突。

```text
P1-1: FIXED / NOT_REPRODUCIBLE_AFTER_FIX
INVALID DUPLICATE REQUEST: REJECTED
CONFLICTING SAME EVENT ID: IDEMPOTENCY_CONFLICT
SEMANTIC JSON OBJECT REORDER: DUPLICATE
DUPLICATE OBJECT KEY: INVALID_SCHEMA / ZERO WRITES
```

### 3.2 Exact envelope-event correlation

既有 schema 的 event `correlation_id` 保存 envelope `eventId`，作为精确关联身份；未新增字段、索引或
migration。完整状态不再使用 event type、payload hash、JSON 内容或 source/type/content tuple 证明。

只有同时满足以下条件才返回 `COMPLETE_MATCH` / `DUPLICATE`：

- envelope canonical 内容与当前已验证请求一致；
- event `correlation_id` 精确等于 envelope `eventId`；
- tenant、trace、run、source 和 event type 与 envelope 一致；
- 不存在多条候选、冲突或歧义。

Repository 状态分类为：

```text
ABSENT
COMPLETE_MATCH
ENVELOPE_ONLY
EVENT_ONLY
ENVELOPE_CONFLICT
EVENT_CONFLICT
AMBIGUOUS_CORRELATION
```

`COMPLETE_MATCH` 之外的 partial/conflicting/ambiguous 状态均 fail-closed；不自动删除、补写、修复或将
其他 `eventId` 下的等价内容用于完成 orphan envelope。

```text
P1-2: FIXED / NOT_REPRODUCIBLE_AFTER_FIX
EXACT COMPLETE PAIR: DUPLICATE
ORPHAN ENVELOPE: FAIL_CLOSED
ORPHAN + UNRELATED SAME CONTENT EVENT: FAIL_CLOSED
EVENT ONLY / EVENT CONFLICT / AMBIGUOUS: FAIL_CLOSED
CROSS TENANT OR SOURCE: NO MATCH
```

## 4. Atomicity and adapter parity

- JDBC exact-correlation 查询、duplicate/conflict resolution、envelope insert 与 routed event append 保持在同一个
  `TransactionTemplate` / `PROPAGATION_REQUIRED` / same-`DataSource` 边界内。
- `DataSource` identity 继续 fail-fast；router/handler 保持 same-thread synchronous execution。
- in-memory adapter 使用相同 exact correlation key，并在同步 unit of work 中 snapshot/restore envelope、event、
  ordering、cleanup/eviction 和 correlation index。
- rollback 继续保证 envelope + event all-or-nothing；safe retry 与 response-loss retry 保持可用。
- commit outcome unknown 继续内部分类、fail-closed 且禁止自动重试。
- 同 key 同内容并发只有一个完整 winner，其余仅在读取到 exact complete pair 后返回 duplicate；同 key 冲突
  内容不得误判 duplicate，也不产生 envelope-only/event-only。

## 5. Security revalidation

```text
Mode: cumulative local diff
Baseline: 16ecded2f3708d69e05afe5f2a4f621c823d8be3
Blocked implementation: 2cb44f406ff618a531d1ca44e2c1e6848750ed75
Final snapshot: codex-security-snapshot/v1:sha256:f4af9a31a77027b6e9b47e4ed241fcb928c64cbf8ee9d65169c21c98a021ddea
Discovery coverage: 10 / 10 FULL-FILE RECEIPTS
Reportable findings: 0
Active P0 / P1: 0 / 0
P1-1: FIXED / NOT_REPRODUCIBLE_AFTER_FIX
P1-2: FIXED / NOT_REPRODUCIBLE_AFTER_FIX
Duplicate-key candidate: FIXED / NOT_REPRODUCIBLE_AFTER_FIX
Result: PASS
```

安全报告由 Codex Security finalizer 从 `scan-manifest.json`、`findings.json` 与 `coverage.json` 生成，未手写
`report.md`。现有 `correlation_id` 无专用索引没有形成当前可报告安全路径：JDBC feedback persistence 默认/
当前 profile 未启用，入口受认证和限流约束，且正常入口不能制造无界同-correlation rows。该项只作为容量/
性能限制保留，不能用于声称生产容量已证明。

CodeRabbit CLI 检查返回 command not found。仓库禁止下载并执行来源脚本，因此没有运行在线安装命令，也没有
把人工或 Codex Security 复核伪装为 CodeRabbit 结果：

```text
CodeRabbit: NOT_EXECUTED / CLI_NOT_INSTALLED / INSTALL_SCRIPT_BLOCKED_BY_POLICY
```

## 6. Validation evidence

| Command / evidence | Result |
| --- | --- |
| 修复后新增 duplicate-key 定向测试 | PASS / 9 tests / 0 failures / 0 errors / 0 skipped |
| `mvn -B -ntp -pl dh-usecase,dh-infra,dh-api,dh-app -am test` | PASS / 15 of 15 Reactor / PostgreSQL Testcontainers executed |
| `mvn -B -ntp test` | PASS / 19 of 19 Reactor / 1286 tests / 0 failures / 0 errors / 0 skipped |
| PostgreSQL / Flyway | PostgreSQL 17.10 / real Testcontainers / 15 migrations validated and applied / schema v15 |
| `mvn -B -ntp -Pquality validate` | PASS / 19 of 19 Reactor / Checkstyle 0 / Spotless PASS |
| Codex Security final contract | PASS / 0 reportable findings / canonical report generated |
| `coderabbit --version` | FAIL / exit 1 / command not found; non-blocking tool availability limitation |

本轮 Maven 的最终通过结果在 strict duplicate-key 修复之后执行。此前由于缺少 `-am`、PowerShell 未引用
`-D` 参数、首次测试预期错误以及一次工具时限终止产生的失败均属于已完成 RCA 的过程证据，不作为最终通过
结果；没有隐藏或改写这些失败。

## 7. Boundary confirmation

```text
API_CHANGE: NONE
MIGRATION_CHANGE: NONE
SCHEMA_CHANGE: NONE
NEW_REPOSITORY_TYPE: NONE
CONTRACTS / POM / WORKFLOW CHANGE: NONE
NQ_CHANGE: NONE
REAL_HTTP / REAL_PROVIDER: NO / NO
AGENT / LANGGRAPH: NO / NO
PAPER / LIVE: NO / NO
FEEDBACK_LEARNING: NO
LEARNING_STORE INBOUND WRITES: 0
CAPACITY GATE: NOT_EXECUTED
PRODUCTION CAPACITY: NOT_PROVEN
PUSH / TAG: NO / NO
FINAL CLOSE: NOT_EXECUTED
```

## 8. Readiness decision

```text
FEEDBACK_INGEST_ATOMICITY_SECURITY_BLOCKER: FIXED
P1_DUPLICATE_VALIDATION_ORDER: PASS
P1_EXACT_EVENT_CORRELATION: PASS
INVALID_DUPLICATE_REQUEST: REJECTED
CONFLICTING_SAME_EVENT_ID: CONFLICT
ORPHAN_WITH_UNRELATED_EVENT: FAIL_CLOSED
ACTIVE_P0_P1: 0
JDBC_IN_MEMORY_PARITY: PASS
FULL_REGRESSION: PASS
QUALITY_GATE: PASS
CODEX_SECURITY_REVALIDATION: PASS
API_CHANGE: NONE
MIGRATION_CHANGE: NONE
SCHEMA_CHANGE: NONE
PRODUCTION_CAPACITY: NOT_PROVEN
ALLOW_FINAL_CLOSE_RETRY: YES
ALLOW_IMPLEMENTATION_PUSH_NOW: NO
ALLOW_FEEDBACK_LEARNING: NO
ALLOW_NQ_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
```

## 9. Risks and rollback

- Exact correlation key sufficiency：依赖既有 `correlation_id = envelope eventId` 约定；回归已覆盖 JDBC、in-memory、
  cross-tenant/source、orphan、conflict 与 ambiguity。未来 schema 或 producer 修改必须同步保持该约定。
- Legacy orphan state：仍保持 fail-closed，不自动修复；如生产数据存在 orphan，需另起经授权的数据治理任务。
- JSON canonicalization：对象 key 重排兼容，重复 key 拒绝；数组顺序与真实值/类型差异保持冲突。
- Concurrent conflicting requests：数据库唯一约束、同事务 read-back 与 in-memory synchronized UoW 保持单 winner；
  生产容量和极限竞争强度仍未证明。
- Commit unknown：维持 no automatic retry，调用方只能按既有 fail-closed 语义处理。
- Repository contract drift：不支持 exact-correlation 的实现使用 port 默认 fail-closed；新增 adapter 必须补齐 parity tests。
- Capacity：`correlation_id` 无专用索引仍是已知容量限制；本任务没有授权 migration 或 capacity gate。

回滚方式：在尚未 push 的前提下，使用新的普通 revert commit 回退本 remediation commit；不得 amend、reset、
rebase 或改写保留的 `2cb44f...`。回滚会重新暴露两条 P1，因此只能在明确接受 security blocker 重新打开的
情况下执行。

## 10. Next concrete action

```text
DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-MILESTONE-FINAL-CLOSE-RETRY
```

下一任务只能重新执行 milestone final-close；当前任务不授权 push、remote exact-SHA CI、archive、tag 或
post-tag cleanup。
