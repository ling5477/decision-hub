# Stage-QDR-10 Evidence Correlation Security Blocker

## 1. Task classification

```text
Task: DH-STAGE-QDR-10-EVIDENCE-CORRELATION-SECURITY-BLOCKER
Type: P3_SECURITY_FIX + EVIDENCE_CORRELATION_HARDENING + BOUNDED_COMPLETENESS
Baseline: 87304e334787d778b10ebad1b1b7f17057f47322
Blocked implementation: 756db5bdb541f94713211848f52e2c223c956dae / LOCAL_ONLY / NOT_PUSHED
Remediation commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT_PUSHED
Stage-QDR-10: IMPLEMENTED / LOCAL_SECURITY_REMEDIATED
Original final-close attempt: BLOCKED / HISTORY PRESERVED
Final close: RETRY PENDING / NOT EXECUTED IN THIS TASK
Production capacity: NOT_PROVEN
```

本任务只修复原 Codex Security 的两个 Low/P3 finding，并补齐相应测试和 current factsources。它不执行
publication、remote exact-SHA CI、archive、tag 或 final close，不授权 API、migration、schema、feedback
learning、NQ、HTTP、Provider、Agent、LangGraph、capacity、Paper 或 LIVE。

## 2. Original blocker evidence

```text
Original scan: 69bf31bc-2e61-4853-b343-902b28c28d91
Coverage: COMPLETE
Active P0/P1: 0 / 0
Reportable findings: 2 LOW/P3
P3-1: legacy decision root has no proven environment provenance
P3-2: COMPLETE aggregate does not preserve bounded evidence policy
Original final-close result: BLOCKED
```

上述 BLOCKED 历史不可删除或改写为 PASS。本任务的安全重扫基线仍是 `87304e3...`，覆盖 blocked
implementation 与本次 remediation 的累计 diff，不只扫描 `756db5b...` 之后的增量。

## 3. Remediation result

### 3.1 Decision environment provenance

- 新增 internal-only `DecisionEnvironmentProvenanceQueryPort` 与 JDBC adapter。
- query 只使用 `tenantId + requestId + traceId + decisionId + decisionRunId`，不接受 caller environment。
- adapter 通过 completed persistent guard、V5 request/output、V6 request/run 的一条 PostgreSQL statement
  snapshot 证明 decision origin environment；不依赖全局 `decisionId` 唯一性。
- missing、ambiguous、invalid、caller/decision mismatch 全部 fail-closed，并在 feedback read 前停止。
- 合法路径强制 `caller environment = persisted decision environment = feedback environment`。
- 不使用默认 `DEV`、Spring profile、tenant/trace/feedback 推断、进程内 registry 或 cache。

### 3.2 Bounded completeness

- 新增 immutable `BoundedEvidencePolicy`，携带 `fromObservedAt`、`toObservedAt`、`maxFeedbackItems`、
  policy ID/version、closed-window semantics、ordering policy 与 fail-closed overflow behavior。
- aggregate 状态改为 `COMPLETE_WITHIN_BOUNDS`、`PARTIAL_WITHIN_BOUNDS`、`INCONSISTENT`、
  `NOT_FOUND`，consumer 只能调用 `isUsableWithinBounds()`。
- query 的 bounds 原样进入 aggregate，不读取系统当前时间；record equality/hash identity 包含完整 policy。
- `hasNext=true` 或第 101 条 evidence 继续 fail-closed；不截断成 complete。

### 3.3 Rejected-evidence non-disclosure

- 任何 feedback validation finding 都只返回空 `feedbackEvidence`。
- aggregate 构造器独立强制 `INCONSISTENT` / `NOT_FOUND` 不得携带非空 feedback。
- overflow、cross-scope、unsafe material、duplicate/order conflict 与 hostile reader 均有非披露回归。

## 4. Validation

```text
Targeted PostgreSQL acceptance: PASS / 7 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Positive environment path: DEV provenance + DEV caller + DEV feedback / PASS
PostgreSQL / Flyway: 17.10 / REAL TESTCONTAINERS / V1-V15
Full regression: PASS / 19 OF 19 REACTOR / 1326 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
git diff --check: PASS
API / migration / schema / POM / workflow: UNCHANGED / NONE
Learning-store production writes: 0
NQ / HTTP / Provider / Agent / LangGraph diff: 0
```

第一次定向 Maven 命令因 PowerShell 将 `-Dsurefire.failIfNoSpecifiedTests=false` 误解析为 lifecycle phase 而
退出 1；该命令未进入测试。使用 PowerShell stop-parsing 形式重跑后通过，不属于代码或测试失败。

## 5. Security revalidation

```text
Sealed remediation scan: 87304e3_worktree_20260809T111514
Sealed snapshot: codex-security-snapshot/v1:sha256:83d733fbbe5718f64d05ce9d9deee424868899623ce7fd95902abc2e0ca94677
Mode: DIFF / BASELINE 87304e334787d778b10ebad1b1b7f17057f47322 -> FINAL WORKTREE SNAPSHOT
Coverage: COMPLETE / 28 OF 28 FULL-FILE RECEIPTS
Deferred: 0
Active P0/P1: 0 / 0
Reportable findings: 0
Original P3-1: FIXED
Original P3-2: FIXED
Intermediate rejected-evidence candidate: FIXED BEFORE FINAL SNAPSHOT
CodeRabbit: CLI UNAVAILABLE IN CURRENT SESSION / NO REVIEW RESULT
```

CodeRabbit 不能替代 Codex Security。当前 session 的 `coderabbit` 命令不可用；此前 implementation 任务的
604 秒 timeout 仍保留为历史，不得冒充本任务 review PASS。

## 6. Files changed by remediation

### Production

```text
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/evidence/JdbcDecisionEnvironmentProvenanceQueryAdapter.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/BoundedEvidencePolicy.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionEnvironmentProvenance.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionEnvironmentProvenanceQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionEnvironmentProvenanceQueryPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceAggregate.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceFinding.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/EvidenceCompleteness.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/PersistentGuardIdentity.java
```

### Tests

```text
dh-app/src/test/java/com/guidinglight/decisionhub/DecisionFeedbackEvidenceConsolidationFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr10/StageQdr10EvidenceArchitectureTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceAggregateTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceQueryTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceServiceTest.java
```

## 7. Boundary and next action

```text
API / Controller / DTO / OpenAPI: UNCHANGED / NONE
Migration / schema: UNCHANGED / NONE
Feedback ingestion / attribution writes: UNCHANGED
Feedback learning: NOT AUTHORIZED
Implementation push: NO
Tag: ABSENT
Final close: NOT EXECUTED
ALLOW_FINAL_CLOSE_RETRY: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_PUSH_NOW: NO
ALLOW_FEEDBACK_LEARNING / ALLOW_REFERENCE_LIVENESS / ALLOW_RETENTION / ALLOW_CAPACITY: NO / NO / NO / NO
ALLOW_NQ_RUNTIME / ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
Next action: DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-FINAL-CLOSE-RETRY
```

本任务结束后只允许另起 final-close retry；该 retry 必须自行冻结 publication、remote exact-SHA CI、
archive 与 tag discipline。本记录本身不授权 push、tag 或 final close。
