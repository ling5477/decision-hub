# Feedback ingest atomicity publication blocker

## 1. Task classification and boundary

```text
Task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-PUBLICATION-BLOCKER
Classification: CI_BLOCKER_FIX / READ_COMMITTED_TORN_READ_FIX
Technical fix: AUTHORIZED
Fix commit / push / exact-SHA CI: AUTHORIZED / AFTER LOCAL REVIEW
Final close / archive / tag: NOT AUTHORIZED
API / migration / schema / new Repository type: NOT AUTHORIZED
```

本任务只修复 feedback ingestion persistence state 的跨 statement snapshot 撕裂读取。不得修改
事务隔离级别、fail-closed 规则、业务顺序、API、migration、schema、contracts、POM、workflow、
NQ、learning store、HTTP/provider、Agent/LangGraph、Paper 或 LIVE。

## 2. Preserved failure evidence

```text
Baseline: 0b686e8ec25b2383a0a84407871928c5097a31c4
Failed exact-SHA CI: 30823448218
Quality job: SUCCESS
Testcontainers build/test: FAILURE
Failing test: FeedbackIngestAtomicityFlywayPostgresTest
  .concurrentSameKeyProducesOneCompleteWinnerAndNoPartialRows
Failure: NqFeedbackIngestionTransactionException
  feedback ingestion has an orphan routed event
```

该失败是真实并发一致性缺陷，不得改写为基础设施故障。根因为 PostgreSQL `READ COMMITTED` 下，
`findIngestionPersistence` 先读 envelope、再读 correlated event；winner 在两个 statement 之间提交时，
loser 可拼接出“旧 envelope snapshot + 新 event snapshot”，并错误分类为 `EVENT_ONLY`。

## 3. Implemented fix

`JdbcNqFeedbackEventRepository.findIngestionPersistence` 现在通过一次 `JdbcTemplate.query` 执行一个
aggregate/CTE SQL statement，并在同一 PostgreSQL statement snapshot 内返回：

- envelope count 与 exact correlated-event count；
- canonical envelope 比较所需的 eventId、type、time、job/trace/request/correlation/schema/payload；
- event tenant、run/trace、source、type 与 correlation identity；
- count 大于 1 时的 ambiguous classification 证据。

状态分类继续保留 `ABSENT`、`COMPLETE_MATCH`、`ENVELOPE_ONLY`、`EVENT_ONLY`、
`ENVELOPE_CONFLICT`、`EVENT_CONFLICT` 与 `AMBIGUOUS_CORRELATION`。真实 orphan、冲突和歧义仍
fail-closed；未增加 retry、sleep、锁、自动删除、自动补写或近似内容匹配。事务继续使用默认
`READ COMMITTED`，冲突仲裁继续使用 `INSERT ... ON CONFLICT DO NOTHING` 与新 statement 回读。

## 4. Regression evidence

| Evidence | Result |
| --- | --- |
| Repository query-count test | PASS / 1 JDBC state-query invocation |
| PostgreSQL focused atomicity test | PASS / 10 tests / 0 failures / 0 errors / 0 skipped |
| Deterministic uncommitted-winner test | PASS / CountDownLatch / no Thread.sleep |
| Original 8-worker same-key test | PASS / 1 ACCEPTED + 7 DUPLICATE / 1 + 1 rows |
| True envelope-only / event-only / ambiguity | PASS / fail-closed |
| Strict JSON / exact correlation / response-loss retry | PASS / preserved |
| `mvn -B -ntp -pl dh-infra,dh-usecase,dh-app -am test` | PASS / 15 of 15 Reactor |
| `mvn -B -ntp test` | PASS / 19 of 19 / 1293 / 0 / 0 / 0 |
| PostgreSQL / Flyway | 17.10 / V1-V15 / real Testcontainers / 0 skipped |
| `mvn -B -ntp -Pquality validate` | PASS / 19 of 19 / Checkstyle 0 / Spotless PASS |

首次 PostgreSQL 定向测试因本机 Docker daemon 未启动而在 Testcontainers 初始化阶段失败；启动本机
Docker Desktop 后，同一命令真实重跑并通过。首次 Maven property 未引用导致 lifecycle 参数解析失败，
加引号后原范围重跑通过。两项过程失败均保留，不冒充代码验证结果。

## 5. Security and boundary review

```text
Manual local diff review: PASS
Cumulative review: 16ecded2f3708d69e05afe5f2a4f621c823d8be3..working tree
Active P0/P1: 0/0
Unauthorized bypass: 0
Codex Security exact commit diff: PENDING / REQUIRED BEFORE PUSH
CodeRabbit: NOT_EXECUTED / CLI_NOT_INSTALLED / INSTALL_SCRIPT_BLOCKED_BY_POLICY
```

单语句读取没有参数拼接，仍由 `JdbcTemplate` positional parameters 绑定；exact event correlation 与
canonical envelope comparison 均保留。当前 schema 没有 `correlation_id` 专用索引，本轮禁止 migration，
因此该项继续作为 capacity/performance limitation；不能据此声称 production capacity 已证明。

## 6. Current authority

```text
FEEDBACK_INGEST_ATOMICITY: IMPLEMENTED / PUBLICATION_CI_BLOCKER_REMEDIATED
FAILED CI: 30823448218 / PRESERVED
ROOT CAUSE: READ_COMMITTED_TORN_STATE_READ
FIX: SINGLE_STATEMENT_INGESTION_STATE_SNAPSHOT
ACTIVE P0/P1: 0 / 0
LOCAL SECURITY REVIEW: PASS
REMOTE FIX CI: PENDING
FINAL CLOSE: BLOCKED / RETRY PENDING
API / MIGRATION / SCHEMA: UNCHANGED / NONE / NONE
PRODUCTION CAPACITY: NOT_PROVEN
```

Fix commit 使用 `THIS_DOCUMENT_COMMIT` 标识；创建后必须对精确 commit diff 运行 Codex Security，
通过后才允许 push。发布与 exact-SHA CI 成功后，本任务仍停止在 `IMPLEMENTATION_FIX: PUBLISHED / CI_GREEN`，
不得进入 final close、archive 或 tag。

## 7. Next concrete action

- fix 发布且 exact-SHA CI 通过：
  `DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-MILESTONE-FINAL-CLOSE-RETRY-3`。
- CI 失败：
  `DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-PUBLICATION-BLOCKER-RETRY`。
- 如修复被证明需要 migration/schema：
  `DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-SCOPE-BLOCKER`。

无论结果如何，`ALLOW_ARCHIVE_NOW` 与 `ALLOW_TAG_NOW` 均为 `NO`。
