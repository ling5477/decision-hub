# DH Stage-QDR-6 Final Close Review Retry

> 任务：`DH-STAGE-QDR-6-FINAL-CLOSE-REVIEW-RETRY`
> 日期：2026-07-11
> 模式：`REVIEW_ONLY + STAGE_FINAL_ACCEPTANCE + CURRENT_FACTSOURCE_RECHECK + SECURITY_BOUNDARY_REVIEW + ARCHIVE_TAG_READINESS`
> HEAD：`e09d5b4baa72c0cf2b1ffcc6b6a38ff20a7be89c`
> 结论：`PASS`

## 1. Scope

本轮重新审查 Stage-QDR-6 B1-B4、V1-V11、tenant/identity/hash/transaction、deterministic replay、internal report、current factsources 与外部授权边界。只记录 retry 结论；不修改 Java 生产代码、Java 测试、migration、API、Repository/JDBC、production port 或 runtime wiring，不创建 archive packet、close docs commit、tag 或 push，不进入 Stage-QDR-7 planning。

持续禁止：real HTTP、real Provider、Provider SDK、NQ runtime integration、Agent/LangGraph/Python runtime、外部 API、交易/执行、Paper、LIVE、raw prompt/raw provider response/credential storage。

## 2. Current fact verification

```text
repository: decision-hub
branch: dev
HEAD: e09d5b4baa72c0cf2b1ffcc6b6a38ff20a7be89c
latest commit: e09d5b4 docs(qdr): align stage-qdr-6 current factsources
pre-review worktree: clean
pre-review staged: empty
B1: DONE / COMMITTED
B2: DONE / COMMITTED
B3: CLOSED / ACCEPTED
B4: DONE / COMMITTED
previous final close review: BLOCKED / CURRENT_FACTSOURCE_CONFLICT / HISTORICAL_PRESERVED
factsource blocker fix: DONE / COMMITTED
archive: NOT_STARTED
tag: NOT_STARTED
Stage-QDR-7: NOT_STARTED / NOT_ALLOWED_YET
current fact mismatches: NONE
```

`README.md`、`docs/current/README.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`、`docs/current/STATUS.md` 与 `docs/current/WORK_ORDER.md` 的 review preflight 状态一致：Stage-QDR-6 implementation complete、final close retry、archive/tag 未开始、Stage-QDR-7 未开始且未授权。用户指定 stale scan 中 `IMPLEMENTATION_NOT_STARTED`、`Stage-QDR-7.*STARTED`、Stage-QDR-6 `ARCHIVED/TAGGED` 均无当前状态命中；`next.*B1` 命中只来自已清除冲突的说明和已消费的 Stage-QDR-5 历史段。

Previous `DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW.md` 仍保留 `BLOCKED` 与原 HEAD `964b493`，本轮未修改。该历史结论记录上轮真实 blocker，不与本次 retry PASS 相互覆盖。

## 3. Code reality inspected

### 3.1 B1 evidence contracts

- `DecisionEvidenceQuery`、`DecisionEvidenceCorrelation` 与 `DecisionEvidencePolicy` 保持 tenant/trace/request/decision exact correlation、固定 completeness profile 与 fail-closed taxonomy。
- missing mandatory evidence 为 `INCOMPLETE`；identity conflict、unsafe ref、raw/credential/trading material 为 `INVALID`。
- B1 20 项 contract tests 与依赖 guard 本轮通过。

结论：`B1_ACCEPTANCE: PASS`。

### 3.2 B2 evidence aggregation

- `DecisionEvidenceAggregateService` 仍只组合现有 V5/V6/V8/V9 ports 与 Stage-QDR-5 internal read models。
- Source read 均 tenant-first；不存在 tenantless scan、latest、fallback 或 fuzzy match。
- Source failure、unknown/unsafe state、cross-tenant 与 correlation conflict 均 fail-closed；不透传 raw payload 或异常文本。
- B2 17 项 aggregation tests 与 source/dependency guards 本轮通过。

结论：`B2_ACCEPTANCE: PASS`。

### 3.3 B3 canonical snapshot、persistence 与 deterministic replay

- V10/V11 仍是 additive canonical snapshot schema 与 metadata-only forward migration；Stage-QDR-6 只新增 V10/V11，V1-V9 历史 migration 未修改，本轮 V1-V11 worktree diff 为空。
- Persistence port 只有 insert、tenant + snapshot ID exact read 与 tenant + full identity exact read；没有 update/delete/latest/list-all/fallback/tenantless query。
- `ReplayInputSnapshotAssemblyService` 在显式 PostgreSQL `REPEATABLE_READ` transaction 中完成 source double-read、assembly、`QDR6-CJSON-1` canonicalization、hash、immutable insert 与 exact read-back；失败触发 rollback。
- Input hash domain 为 `DH-QDR6-CANONICAL-REPLAY-SNAPSHOT`；output hash domain 为 `DH-QDR6-DETERMINISTIC-REPLAY-OUTPUT`，并绑定各自 schema/version vector，不允许 input/output hash 混用。
- `DeterministicReplayExecutor` 只接受 `QDR6-MOCK-REPLAY-1`，唯一 IO 是 tenant-bound persisted snapshot exact read；不读 mutable source、不写库、不调用 HTTP/Provider/NQ/Agent/LangGraph/交易执行。
- `REPRODUCIBLE` 仅代表相同 persisted structured snapshot 的本地 mock output 可复现，不代表真实 LLM/provider 可复现、业务决策正确或获得授权。

结论：`B3_ACCEPTANCE: PASS`。

### 3.4 B4 internal report

- `DecisionEvidenceReplayReportService` 完整映射 evidence、deterministic replay、regression、provider readiness 与 observability structured results。
- Missing、invalid、unsupported、source failure、identity mismatch 或任一非通过状态均按固定优先级 fail-closed。
- `InternalAcceptanceStatus.ACCEPTED` 只表示内部 evidence/replay acceptance；所有状态的 Provider、NQ、trading/execution 与 Paper/LIVE authorization helper 均固定返回 `false`。
- Report 只保留 safe refs、hash、status、taxonomy finding 与 sanitized message；拒绝 raw prompt、raw provider response、credential-like material 和 executable trading material。

结论：`B4_ACCEPTANCE: PASS`。

## 4. Security boundary review

代码、dependency surface、architecture guards 与本轮测试共同确认 Stage-QDR-6 未新增或启用：

```text
真实 HTTP
真实 Provider 或 Provider SDK
NQ runtime integration
Agent / LangGraph / Python runtime
外部 API
交易、下单、撤单或执行能力
Paper 或 LIVE 授权
raw prompt / raw provider response / credential storage
```

`NO_EXTERNAL_IO` 表示没有外部网络/provider/NQ/Agent/交易 IO；deterministic replay 按设计保留一次既有 tenant-bound PostgreSQL exact read。Internal acceptance、reproducibility 与 final close PASS 都不构成任何外部授权。

结论：`SECURITY_BOUNDARY: PASS`。

## 5. Validation evidence

```text
git status --short: PASS / pre-review clean
git diff --check: PASS
git log --oneline -20: PASS / HEAD e09d5b4
mvn -ntp -pl dh-usecase,dh-infra -am test: PASS / 0 failures / 0 errors / 0 skipped
mvn -ntp -pl dh-app -am test: PASS / dh-app 106 tests / 0 skipped
mvn -ntp test: PASS / 1017 tests / 0 failures / 0 errors / 0 skipped
mvn -ntp -Pquality validate: PASS / reactor 19 of 19
Checkstyle: PASS / 0 violations
Spotless: PASS
ArchitectureTest: PASS / 39 tests / 0 skipped
PostgreSQL/Testcontainers: PASS / Docker Desktop / postgres:17 / PostgreSQL 17.10
Flyway: PASS / V1-V11 validated and applied
V10CanonicalReplaySnapshotFlywayPostgresTest: PASS / 16 tests / 0 skipped
V1-V9 historical migration diff: empty
V1-V11 worktree diff: empty
forbidden-scope diff: empty
pre-review staged: empty
commit/push/archive/tag: NOT_RUN / NOT_CREATED
```

系统 Maven `mvn` 可用。Mockito/ByteBuddy 动态 agent future-JDK warning 为 non-blocking tooling risk；`mvnw.cmd` 的既有 wrapper risk 未在本轮修复，不影响系统 Maven 验收。

## 6. Readiness decision

```text
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS
CURRENT_FACTSOURCE_CONSISTENCY: PASS
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

ALLOW_STAGE_QDR_6_ARCHIVE_PACKET: YES
ALLOW_STAGE_QDR_6_CLOSE_DOCS_COMMIT: NO
ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: YES
ALLOW_STAGE_QDR_7_PLAN_NOW: NO

ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_LIVE: NO
```

`ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: YES` 仅表示 archive packet 完整、archive commit 存在且工作区 clean 后可进入 tag close；不表示本轮可创建 tag。Archive、close docs commit、tag 与 push 均由下一任务按 `archive-before-tag` 顺序执行。

## 7. Risks

- current factsource drift：下一任务必须同步 review PASS、archive/tag state，且不得改写 previous BLOCKED 历史。
- evidence contract drift：新增 evidence type 必须同步 mandatory profile、taxonomy 与 tests。
- aggregation coverage：source schema/identity 变化必须继续 fail-closed，禁止 fallback。
- snapshot/version drift：新版本必须显式 compatibility review，禁止 moving aliases。
- migration compatibility：本轮证明本地 clean/upgrade path；外部历史环境仍须在实际升级窗口验证数据兼容性。
- canonicalization/hash drift：canonicalizer、domain prefix、field order 或 version vector 变化必须版本化。
- false reproducibility：`REPRODUCIBLE` 不等于真实 Provider/LLM 可复现或业务正确。
- internal acceptance misuse：`ACCEPTED` 不得映射为 Provider、NQ、交易、执行、Paper 或 LIVE 授权。
- tenant isolation：新增读取必须保持 tenant-first exact identity。
- external IO leakage：未来 API/wiring/provider 需求必须另起安全 review。
- external environment compatibility：本轮 PostgreSQL evidence 来自本地 Docker Desktop + PostgreSQL 17.10。
- Testcontainers dependency：archive/tag 前复验仍依赖 Docker 可用。
- Maven wrapper risk：继续使用系统 Maven，wrapper 修复需另起 tooling task。

## 8. Next concrete action

```text
DH-STAGE-QDR-6-ARCHIVE-TAG-CLOSE
```

下一任务必须先形成并提交 self-contained archive packet，再在 clean worktree 上创建 annotated tag；不得并行启动 Stage-QDR-7 planning，不得借 archive/tag close 授权 Provider、NQ、Agent/LangGraph、交易或 LIVE。
