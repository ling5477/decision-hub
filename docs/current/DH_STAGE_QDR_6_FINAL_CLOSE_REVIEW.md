# DH Stage-QDR-6 Final Close Review

> 任务：`DH-STAGE-QDR-6-FINAL-CLOSE-REVIEW`
> 日期：2026-07-11
> 模式：`REVIEW_ONLY + STAGE_FINAL_ACCEPTANCE + SECURITY_BOUNDARY_REVIEW + ARCHIVE_TAG_READINESS`
> HEAD：`964b49305d02a54e76803cb8f62d706d187aa448`
> 结论：`BLOCKED`

## 1. 审查范围与禁止项

本轮只读审查 Stage-QDR-6 B1–B4、V10/V11、tenant/identity/hash/transaction、deterministic replay、internal report 与外部授权边界。除本 review 与 current facts 同步外，不修改 Java 生产代码、Java 测试、migration、API、Repository/JDBC、production port 或 runtime wiring；不创建 archive packet、不提交、不打 tag、不 push。

持续禁止：real HTTP、real Provider、Provider SDK、NQ runtime integration、Agent/LangGraph/Python runtime、外部 API、交易/执行、Paper 与 LIVE。

## 2. Current fact verification

```text
repository: E:\Project\decision-hub
branch: dev
HEAD: 964b49305d02a54e76803cb8f62d706d187aa448
latest commit: 964b493 feat(qdr): add evidence replay internal report
pre-review worktree: clean
pre-review staged: empty
Stage-QDR-6 B1: DONE / COMMITTED
Stage-QDR-6 B2: DONE / COMMITTED
Stage-QDR-6 B3: CLOSED / ACCEPTED
Stage-QDR-6 B4: DONE / COMMITTED / 964b493
Stage-QDR-6 final close review: BLOCKED
```

`README.md`、`docs/current/README.md` 与 `docs/current/CODEX_PROJECT_INSTRUCTIONS.md` 仍包含 Stage-QDR-6 `IMPLEMENTATION_NOT_STARTED`、只允许 B1 或 next B1 的旧口径。`FACTSOURCE_POLICY.md` 明确把这三份文件列为可阻断 close 的 current factsources；它们与 HEAD、代码事实、`STATUS.md`、`WORK_ORDER.md` 冲突，且不在本轮 allowlist，无法在本任务修复。因此形成 `CURRENT_FACTSOURCE_CONFLICT_BLOCKED`：实现与安全验收可分别 PASS，但 final close、archive packet 与 tag readiness 必须 fail-closed。

## 3. B1 Evidence Contracts

- `DecisionEvidenceQuery` 与 `DecisionEvidenceCorrelation` 强制 `tenantId + traceId + requestId + decisionId`，缺失、空白、cross-tenant 或 correlation conflict 均拒绝或得到 `INVALID`。
- `DecisionEvidencePolicy` 将 mandatory evidence 缺失稳定映射为 `INCOMPLETE`，冲突、unsafe ref、credential-like、raw prompt/raw provider response 或交易语义映射为 `INVALID`；满足固定 profile 才能为 `COMPLETE`。
- `DecisionEvidenceRef` 复用既有 `DecisionEvidence` / `DecisionEvidenceView`，没有复制 decision/QDR/replay 模型，也不读取 raw payload。
- B1 20 项 contract tests 与依赖 guard 全部通过。

结论：`B1_ACCEPTANCE: PASS`。

## 4. B2 Evidence Aggregation

- `DecisionEvidenceAggregateService` 只组合既有 V5 `DecisionReplayQueryRepository`、V6 `DecisionReadModelQueryPort`、V8 gateway persistence port、V9 replay/evaluation/verdict ports 与 Stage-QDR-5 内部 read models。
- 所有来源查询 tenant-first；V5 使用完整四键，V6/V8 仅在 exact selector/safe ref 下读取，V9 使用 tenant + exact ID/trace 路径；没有 tenantless scan、latest、fallback 或模糊匹配。
- mandatory evidence 缺失为 `INCOMPLETE`；source exception、未知/不可安全状态、identity conflict、cross-tenant 或 unsafe mapping 为 `INVALID`。
- B2 没有新增 Repository/JDBC/schema；17 项 aggregation tests 与 source/dependency guards 全部通过。

结论：`B2_ACCEPTANCE: PASS`。

## 5. B3 Canonical Snapshot 与 Deterministic Replay

- V10 是 additive migration；V11 只补充 V10 constraint/index 审计 COMMENT。Stage-QDR-6 基线 `5108f24..HEAD` 对 V1–V9 diff 为空，本轮 worktree 对 V1–V11 diff 为空。
- V10 建立 tenant-aware FK/unique、严格 structured JSON shape、unsafe-key gate、256 KiB 总 payload 与分字段限制、固定 version vector、hash constraints 和 UPDATE rejection trigger。Persistence port 仅提供 insert 与 tenant-bound exact reads，不提供 update/delete/latest/list-all/fallback/tenantless 查询。
- `Qdr6CanonicalJson` 固定 `QDR6-CJSON-1`；input hash 与 replay output hash 使用不同 domain prefix 的 SHA-256。Output hash 还绑定 output/snapshot/canonical/executor/hash version。
- `ReplayInputSnapshotAssemblyService` 在显式 PostgreSQL `REPEATABLE_READ` transaction 内完成两次 source load、assembly/hash 比对、immutable insert 与 exact read-back；异常触发 rollback。PostgreSQL tests 已真实验证 isolation、rollback、duplicate-identical、conflict、tenant isolation 与 source drift。
- `DeterministicReplayExecutor` 只接受 `QDR6-MOCK-REPLAY-1`，唯一 IO 是 tenant-bound persisted snapshot exact read；不写 replay result、不调用 HTTP/Provider/NQ/Agent/LangGraph/交易执行。
- `REPRODUCIBLE` 仅表示相同 persisted structured snapshot 的本地 mock output 可复现，不表示真实 LLM/provider 可复现，也不表示业务决策正确或获得任何授权。

结论：`B3_ACCEPTANCE: PASS`。

## 6. B4 Internal Report

- `DecisionEvidenceReplayReportService` 完整映射 evidence、deterministic replay、regression、provider readiness 与 observability structured results。
- missing、unknown/unsupported、invalid、source failure、identity mismatch、replay difference、regression/readiness/observability 非通过状态均按冻结优先级 fail-closed。
- `InternalAcceptanceStatus.ACCEPTED` 只表示内部 evidence/replay 验收；所有状态的 provider/NQ/trading/execution/Paper/LIVE authorization helper 固定返回 `false`。
- Report 只保存 safe refs、hash、状态、taxonomy finding 与 sanitized message；raw prompt、raw provider response、credential-like material 会被拒绝，source exception 文本不会透传。
- B4 16 项 tests 与 package dependency guard 全部通过。

结论：`B4_ACCEPTANCE: PASS`。

## 7. Security boundary review

代码、依赖 surface、contract/architecture guards 与测试证据共同确认 Stage-QDR-6 未实现：

```text
真实 HTTP
真实 Provider 或 Provider SDK
NQ runtime integration
Agent / LangGraph / Python runtime
外部 API
交易、下单、撤单或执行能力
Paper 或 LIVE 授权
raw prompt / raw provider response / credential persistence
```

`NO_EXTERNAL_IO` 的含义是没有外部网络/provider/NQ/Agent/交易 IO；B3 replay 仍按设计执行一次既有 tenant-bound PostgreSQL snapshot exact read。

结论：`SECURITY_BOUNDARY: PASS`。

## 8. Validation evidence

```text
git status --short: pre-review clean
git diff --check: PASS
git log --oneline -20: PASS；HEAD 与 B1-B4 commits 已核验
mvn -ntp -pl dh-usecase,dh-infra -am test: PASS
mvn -ntp -pl dh-app -am test: PASS
mvn -ntp test: PASS / 1017 tests / 0 failures / 0 errors / 0 skipped
mvn -ntp -Pquality validate: PASS
Checkstyle: PASS / 0 violations
Spotless: PASS
ArchitectureTest: PASS / 39 tests / 0 skipped
PostgreSQL/Testcontainers: PASS / Docker Desktop / postgres:17 / PostgreSQL 17.10
Flyway: PASS / V1-V11 validated and applied
V10CanonicalReplaySnapshotFlywayPostgresTest: PASS / 16 tests / 0 skipped
staged: empty
commit: NOT CREATED
push: NOT RUN
tag: NOT CREATED
```

系统 Maven `mvn` 可用；`mvnw.cmd -v` 仍失败，错误包括 `'\' is not recognized as an internal or external command` 与 wrapper jar 缺少主清单属性，记录为 P2 tooling risk，不影响本轮系统 Maven 验收。

## 9. Readiness decision

```text
STAGE_QDR_6_FINAL_CLOSE_REVIEW: BLOCKED
B1_ACCEPTANCE: PASS
B2_ACCEPTANCE: PASS
B3_ACCEPTANCE: PASS
B4_ACCEPTANCE: PASS
MIGRATION_INTEGRITY: PASS
TENANT_ISOLATION: PASS
DETERMINISTIC_REPLAY_BOUNDARY: PASS
INTERNAL_REPORT_BOUNDARY: PASS
SECURITY_BOUNDARY: PASS
POSTGRESQL_TEST_EVIDENCE: PASS

ALLOW_STAGE_QDR_6_ARCHIVE_PACKET: NO
ALLOW_STAGE_QDR_6_CLOSE_DOCS_COMMIT: NO
ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: NO
ALLOW_STAGE_QDR_7_PLAN_NOW: NO

ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_LIVE: NO
```

Blocker：`README.md`、`docs/current/README.md` 与 `docs/current/CODEX_PROJECT_INSTRUCTIONS.md` 的 current-state wording 与代码现实冲突。必须在独立 blocker-fix 任务中扩展文档 allowlist、同步 current facts、重跑本 review 的 Git/authority checks 后，才可重新判定 archive/tag readiness。

## 10. Risks

- evidence contract drift：后续若增加 evidence type，必须同步 mandatory profile、stable taxonomy 与 tests。
- aggregation coverage：既有 source schema/identity 变化可能导致 fail-closed `INVALID/INCOMPLETE`，不得增加 fallback。
- snapshot/version drift：任何新 version 必须显式 compatibility review，禁止 `latest/current/default`。
- migration compatibility：V10 增加 source unique constraints，外部历史环境仍须在实际升级窗口验证数据兼容性。
- canonicalization/hash drift：canonicalizer、domain prefix、field order 或 version vector 改动必须新版本化。
- false reproducibility：`REPRODUCIBLE` 不等于真实 LLM 可复现或业务决策正确。
- internal acceptance misuse：`ACCEPTED` 不得映射为 Provider、NQ、交易、执行、Paper 或 LIVE 授权。
- tenant isolation：所有新增读取必须保持 tenant-first exact identity。
- external IO leakage：未来 wiring/API/Provider 需求必须另起安全 review，不得复用本次 PASS 越权。
- external environment compatibility：本轮只验证本地 Docker Desktop + PostgreSQL 17.10。
- Testcontainers dependency：后续 archive/tag 前复验仍依赖 Docker 可用。
- Maven wrapper risk：`mvnw.cmd` 当前不可用，继续使用系统 Maven 或另起 tooling fix。

## 11. Next concrete action

```text
DH-STAGE-QDR-6-FINAL-CLOSE-BLOCKER-FIX
```

下一任务只修复 current factsource 冲突并重跑 final close review；不得借 blocker fix 创建 archive packet、tag、代码、migration、API、Provider、NQ、Agent/LangGraph、交易或 LIVE 能力。
