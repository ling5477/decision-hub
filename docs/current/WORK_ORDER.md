# Decision Hub 当前工单

## 1. 唯一下一步

```text
current task: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW / PASS
next action: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN
mode: REVIEW_ONLY + CLOSE_REVIEW + MOCK_GATEWAY_REGRESSION_REVIEW + QDR_PIPELINE_REVIEW + REPOSITORY_REUSE_REVIEW + REDACTION_REVIEW + TRADING_TERM_GUARD_REVIEW + TEST_EVIDENCE_REVIEW + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B2_PERSISTENCE_BASELINE / DONE
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
stage-qdr-4 B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
stage-qdr-4 B3 plan: DONE / PLAN_ONLY
STAGE_QDR_4_B3_PLAN: DONE
stage-qdr-4 B3 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
stage-qdr-4 B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES
stage-qdr-4 B4 implementation: NOT_STARTED
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
current workspace: E:/Project/decision-hub
```

## 2. 当前前置状态

```text
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B2_PERSISTENCE_BASELINE / DONE
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
stage-qdr-4 B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
stage-qdr-4 B2 close review: PASS
stage-qdr-4 B2: CLOSED / ACCEPTED
stage-qdr-4 B3 plan: DONE / PLAN_ONLY
stage-qdr-4 B3 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
stage-qdr-4 B3 close review: PASS
stage-qdr-4 B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES
stage-qdr-4 B4 implementation: NOT_STARTED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

## 3. B1 完成范围

```text
B1 task: DH-STAGE-QDR-4-B1-REPLAY-EVALUATION-DOMAIN-CONTRACTS
B1 status: DONE
B1 scope: dh-domain + dh-usecase replay/evaluation contracts and unit tests only
B1 persistence: NO
B1 API / Controller: NO
B1 real HTTP / provider / SDK: NO
B1 Agent / LangGraph / LIVE: NO / DISABLED
```

## 4. B2 / B3 完成范围与下一步

```text
DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
已新增 dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
已新增 ReplayCaseRepository / EvaluationCaseRepository / RegressionVerdictRepository 及对应 JDBC adapter
已补充 migration / repository / tenant isolation / redaction / fail-closed 测试
V9 PostgreSQL/Flyway load test 已通过真实 Testcontainers PostgreSQL 验证，不能再按 skip/blocker 处理
close review 已确认 V9 migration、tenant-bound repository、redaction guard、trading-term guard、测试证据和 safety scan 可接受
DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN: DONE / PLAN_ONLY
已规划 dry-run / mock gateway safe refs -> replay case -> evaluation case -> regression verdict / finding 的 future implementation flow
已规划 MockGatewayRegressionCaseBuilder / QdrRegressionEvaluationService / QdrRegressionComparator / RegressionBaselinePolicy / RegressionEvidenceRef 边界
已规划 comparison rules、V9 persistence reuse、redaction/trading guard、B3 implementation test matrix
DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO: DONE / WORK_ORDER_ONLY
B3 WO 已冻结 implementation target flow、usecase/service boundary、comparison rules、B2 persistence reuse、redaction/trading guard、fail-closed 行为、测试矩阵、validation 与 close review 顺序
B3 implementation 已实现 deterministic mock gateway regression flow：existing dry-run / QDR decision artifact -> mock model gateway summary -> replay case -> evaluation case -> expected/actual summary -> regression comparison -> regression verdict -> finding list
已新增 MockGatewayRegressionCaseBuilder / QdrRegressionEvaluationService / QdrRegressionComparator / RegressionBaselinePolicy / RegressionEvidenceRef
已复用 ReplayCaseRepository / EvaluationCaseRepository / RegressionVerdictRepository B2 ports；未新增 schema、未修改 V9
已补充 comparator 与 service regression tests，覆盖 PASS/WARN/FAIL/SKIPPED、drift、redaction、trading-term、cross-tenant、repository failure 和 forbidden runtime scan
B3 close review 已完成并判定 PASS；B3 当前状态为 CLOSED / ACCEPTED
下一步只允许 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN
不得新增 API / Controller，除非后续 B4 plan/WO 明确授权并完成对应 review
不得跳过 B4 plan/WO 直接 implementation
保持 no real HTTP / provider / SDK / Agent / LangGraph / LIVE
```

## 5. Stage-qdr-4 后续禁止范围

```text
禁止未经明确授权继续修改 Java 生产代码
禁止未经明确授权继续修改 Java 测试代码
禁止在 B2 implementation 范围外新增 migration
禁止修改 V1-V8 migration
禁止新增 V9 以外的 migration
禁止新增 API / Controller / REST endpoint
禁止新增真实 HTTP outbound
禁止新增真实 provider client
禁止新增 Provider SDK
禁止启动 LangGraph / AutoGen / CrewAI
禁止启动 Agent runtime
禁止修改 NQ
禁止 B2 与 B3 合并实施
禁止 B3 plan 后跳过 WO 直接 implementation
禁止 B3 与 B4 合并实施
禁止 B4 plan 前进入 B4 implementation
禁止启用 LIVE
禁止 git push
禁止 git commit，除非用户另行明确授权
```

## 6. 可阻断事实源

stage-qdr-4 planning 只应以 `FACTSOURCE_POLICY.md` 中的 current factsource 集合作为默认事实源。`WORKLOG.md`、`ROADMAP.md`、`API.md`、`DB_SCHEMA.md` 默认 supporting only。`docs/gates/**` 为 historical records，不作为默认 blocker；`docs/archive/**` 不再作为 QDR 当前归档标准。

## 7. 验收命令

```powershell
git status --short
git diff --check
git diff --stat
git diff --name-only
git diff --cached --name-only
mvn -ntp -Pquality validate
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
.\mvnw.cmd -v
```

`mvnw.cmd` 当前仍为 `UNUSABLE / P2 TOOLING RISK`；验证使用系统 Maven `mvn`。不得使用 `-DskipTests` 或 `-DskipITs` 后写成完整通过。
