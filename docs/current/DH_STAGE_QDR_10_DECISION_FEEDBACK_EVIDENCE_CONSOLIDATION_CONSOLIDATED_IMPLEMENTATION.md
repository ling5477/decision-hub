# Stage-QDR-10 Decision Feedback Evidence Consolidation — Consolidated Implementation

## 1. Task classification

```text
Task: DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-CONSOLIDATED-IMPLEMENTATION
Type: CODE_CHANGE + READ_ONLY_EVIDENCE_CONSOLIDATION + POSTGRESQL_ACCEPTANCE + CURRENT_FACTSOURCE_SYNC
Stage: DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION
Result: IMPLEMENTED / LOCAL_ACCEPTED
Implementation commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT_PUSHED
Remote implementation CI: PENDING
Stage final close: NOT_STARTED
Production capacity: NOT_PROVEN
```

本记录只关闭 consolidated implementation 本地批次，不关闭 Stage-QDR-10，不发布 implementation，
不创建 tag，也不授权 capacity、reference-liveness、retention、feedback learning、NQ runtime、HTTP、
Provider、Agent、LangGraph、Paper 或 LIVE。

## 2. Baseline publication and exact-SHA CI

```text
Work-order SHA: 87304e334787d778b10ebad1b1b7f17057f47322
Local / origin / advertised SHA: 87304e334787d778b10ebad1b1b7f17057f47322
Publication: PASS / FAST_FORWARD
Exact-SHA CI run: 31259505782 / PASS
Mandatory quality job: PASS
Mandatory Testcontainers/Docker job: PASS
Exact-SHA tests: 1293 / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PostgreSQL / Flyway: 17.10 / V1-V15
Scope invariants: PASS / 3 OF 3
```

只有上述 baseline publication 与 exact-SHA CI 全部通过后才开始本地 implementation。

## 3. Implementation result

### 3.1 Trusted query and correlation

- `DecisionFeedbackEvidenceQuery` 只接受一个 `FeedbackExecutionScope`，不重复接受独立 tenant/environment。
- environment 由 trusted caller 显式提供；不从 Spring profile、tenant、decision、trace、repository 命中、
  latest feedback 或默认 `DEV` 推断。
- decision correlation 固定为 `tenant + trace + request + decision + run`。
- feedback correlation 固定为 `tenant + environment + decision + trace`；合法多观察由
  `observationId + attributionId` 区分。
- query 构造期强制 90 天以内的观察窗口与 `1..100` 的单页上限。

### 3.2 Bounded read and aggregate

- consolidated service 只组合既有 `DecisionEvidenceAggregateService` 与
  `HistoricalFeedbackEvidenceReadService`。
- historical feedback 只读一次；`hasNext=true` 映射为
  `INCONSISTENT / FEEDBACK_RESULT_LIMIT_EXCEEDED`，不循环翻页、不静默截断。
- aggregate 为 immutable、defensive-copy、stable-order、read-only；不保存 raw prompt、raw provider
  response、credential-like material 或交易/执行授权。
- feedback 为空仅返回 `PARTIAL`；存在且一致返回 `COMPLETE`。
- mandatory decision root 缺失返回 `NOT_FOUND`；scope、identity、order、duplicate、unsafe material、
  source failure 或 overflow 返回 `INCONSISTENT`。
- `INCONSISTENT` / `NOT_FOUND` 必须携带 `ERROR` 或 `BLOCKER` finding，`isUsable=false`。

### 3.3 Internal wiring and side-effect boundary

- `DecisionPipelineWiringConfig` 新增 internal `DecisionFeedbackEvidenceService` bean。
- production constructor 只依赖两个既有 read service；未新增 Controller、API、DTO、OpenAPI、migration、
  schema、write Repository、HTTP/Provider/NQ client、Agent/LangGraph 或 trading dependency。
- consolidated path 的 DB writes、external side effects 与 learning mutations 均为 0。

## 4. Files changed

### Production

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceAggregate.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/EvidenceCompleteness.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceFinding.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceService.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
```

### Tests

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceQueryTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceAggregateTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceServiceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/DecisionFeedbackEvidenceConsolidationFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr10/StageQdr10EvidenceArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
```

### Current factsources

```text
docs/current/DH_STAGE_QDR_10_DECISION_FEEDBACK_EVIDENCE_CONSOLIDATION_CONSOLIDATED_IMPLEMENTATION.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

## 5. Validation evidence

```text
Targeted usecase tests: PASS / 17 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Targeted wiring + architecture tests: PASS / 6 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PostgreSQL acceptance: PASS / 3 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PostgreSQL server: 17.10 / REAL TESTCONTAINERS EXECUTION
Flyway: V1-V15 / APPLIED
Module regression: PASS / 15 OF 15 REACTOR
Full regression: PASS / 19 OF 19 REACTOR
Full test total: 1317 / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Quality: PASS / 19 OF 19 REACTOR
Checkstyle / Spotless: 0 / PASS
git diff --check: PASS
Technical allowlist before factsource sync: 12 EXPECTED / 0 UNEXPECTED / 0 MISSING
Forbidden technical path diff before factsource sync: 0
```

PostgreSQL acceptance 覆盖 tenant/environment isolation、decision/trace correlation、0/1/100/>100、
90-day cutoff、stable order、overflow fail-closed 与 aggregate query 零写。全量回归的 Surefire XML
按本轮 `2026-08-08 22:16:20` 至 `22:20:48` 时间范围汇总；`target/ci-diagnostics` 中 2026-07-21
的历史 error 报告不属于本轮，未删除也未计入本轮结果。

## 6. External review result

```text
GitHub baseline verification: PASS
Codex Security implementation review: NOT_EXECUTED / FINAL-CLOSE INDEPENDENT REVIEW NOT_STARTED
CodeRabbit CLI: 0.6.5 / AUTHENTICATED
CodeRabbit uncommitted review: TIMED_OUT / 604 SECONDS / NO NDJSON RESULT
CodeRabbit issues: UNKNOWN / NO RESULT
```

CodeRabbit 已等待完整 10 分钟，但没有返回 finding 或 error event；残留 review 进程已终止。不得把该结果
记为 PASS，也不得把本地检查冒充 CodeRabbit review。该外部工具超时不改变已执行 Maven/PostgreSQL/
architecture assertions 的真实结果，final-close task 必须独立决定 review 与 security gate。

## 7. Boundary confirmation

```text
Unexpected files: 0
API / Controller / DTO / OpenAPI diff: 0
Migration / schema diff: 0
Write persistence diff: 0
Contracts / golden_cases diff: 0
POM / workflow diff: 0
NQ / HTTP / Provider diff: 0
Agent / LangGraph diff: 0
Learning-store write diff: 0
Scope invariants: PASS / 3 OF 3
Feedback learning: NOT AUTHORIZED
Implementation push: NO
Tag: ABSENT
Stage final close: NOT_STARTED
Formal capacity: NOT_EXECUTED
Production capacity: NOT_PROVEN
```

## 8. Risks

- trusted environment provenance 依赖调用方传入已经认证产生的 `FeedbackExecutionScope`；service 不自行认证。
- legacy V5/V6 decision persistence 没有 environment 列；本 stage 通过 trusted caller scope 与严格 decision
  identity 组合保持 fail-closed，但不声称补齐持久化 environment。
- 超过调用方 `maxFeedbackItems` 的 correlated feedback 统一 fail-closed；不提供自动分页或降级。
- CodeRabbit 本轮超时，未形成外部 AI review 结果；final close 需重新评估 review gate。
- formal capacity 未执行，production capacity 与 production readiness 均未证明。

## 9. Rollback

实现尚未 push。需要回滚时，仅对 `THIS_DOCUMENT_COMMIT` 执行普通 `git revert <implementation-sha>`；
不得改写已经发布的 baseline `87304e334787d778b10ebad1b1b7f17057f47322` 历史。

## 10. Readiness decision

```text
STAGE_QDR_10_CONSOLIDATED_IMPLEMENTATION: DONE / LOCAL_ACCEPTED
TRUSTED_ENVIRONMENT_SOURCE: PASS
ENVIRONMENT_INFERENCE: NONE
DECISION_CORRELATION: PASS
FEEDBACK_CORRELATION: PASS
BOUNDED_FEEDBACK_READ: PASS
OVERFLOW_FAIL_CLOSED: PASS
CONSOLIDATED_AGGREGATE: PASS
TENANT_ISOLATION: PASS
ENVIRONMENT_ISOLATION: PASS
NO_SIDE_EFFECT: PASS
FEEDBACK_LEARNING_CONTAINMENT: PASS
FULL_REGRESSION: PASS
QUALITY_GATE: PASS
API_CHANGE: NONE
MIGRATION_CHANGE: NONE
WRITE_REPOSITORY_CHANGE: NONE
PRODUCTION_CAPACITY: NOT_PROVEN
ALLOW_STAGE_QDR_10_FINAL_CLOSE: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_PUSH_NOW: NO
ALLOW_CAPACITY / ALLOW_REFERENCE_LIVENESS / ALLOW_RETENTION / ALLOW_FEEDBACK_LEARNING: NO / NO / NO / NO
ALLOW_NQ_RUNTIME / ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER: NO / NO / NO
ALLOW_AGENT_PHASE / ALLOW_LANGGRAPH_RUNTIME / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

## 11. Next concrete action

```text
DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-FINAL-CLOSE
```

final close 必须另起任务，重新冻结 publication、exact-SHA CI、security/review、archive 与 tag discipline；
本 implementation task 不得自行 push、archive、tag 或进入下一阶段。
