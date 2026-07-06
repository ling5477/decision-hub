# DH stage-qdr-2 Discipline Closeout

```text
Task: DH-STAGE-QDR-2-DISCIPLINE-CLOSEOUT
Task type: DOCUMENTATION + TOOLING_REVIEW + PROCESS_DISCIPLINE_CLOSEOUT + STAGE_STATE_ALIGNMENT + NO_BUSINESS_CODE_CHANGE
Status: DONE / DOCS_AND_TOOLING_DISCIPLINE / NO_RUNTIME
Fact source: docs/current
Date: 2026-07-06
```

## 1. 结论

本轮只关闭 stage-qdr-2 B4 freeze 之后、B5 close review 之前的工程纪律缺口，不启动 B5 review，不新增业务能力。

```text
stage-qdr-2 B1: CLOSED / ACCEPTED / COMMITTED
stage-qdr-2 B2: CLOSED / ACCEPTED / COMMITTED
stage-qdr-2 B3: CLOSED / ACCEPTED / COMMITTED
stage-qdr-2 B4: CLOSED / ACCEPTED / COMMITTED
stage-qdr-2 B5: READY / NOT STARTED / REVIEW_ONLY
stage-qdr-2 overall: IMPLEMENTED_PENDING_CLOSE_REVIEW
stage-qdr-3: NOT STARTED
Replay execution API: NOT STARTED
Model gateway: NOT STARTED
Tool registry: NOT STARTED
Real HTTP outbound: NO
Real provider: NO
Agent / LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

B5 的唯一用途是 close review / acceptance。B5 不得补新功能，不得新增 API、Controller、migration、replay execution、model gateway、provider、真实 HTTP、Agent runtime、LangGraph runtime 或 LIVE。

## 2. 路径纪律

```text
Current DH workspace: E:/Project/decision-hub
Historical path only: F:/project/decision-hub
```

后续提示词引用 DH 仓库路径时，默认必须使用 `E:/Project/decision-hub`。如果不同机器存在路径差异，任务必须先做路径确认；不得自动切换到 `F:/project/decision-hub`，也不得在两个工作区同时存在时继续执行。

## 3. 提交状态

本轮开工前工作区为 clean，当前分支为 `dev`，HEAD 包含 B4 commit：

```text
1e3acf4 feat(qdr): add stage-qdr-2 human approval API
```

本轮不提交、不 push。建议后续如需提交本轮文档：

```text
docs(qdr): close stage-qdr-2 discipline gaps
```

## 4. Maven wrapper 风险

`.\mvnw.cmd -v` 当前不可用，不能写成 Maven Wrapper PASS。

```text
Command: .\mvnw.cmd -v
Exit code: 0
Observed output:
  '\' is not recognized as an internal or external command
  .mvn\wrapper\maven-wrapper.jar 中没有主清单属性
Decision: mvnw.cmd UNUSABLE / P2 TOOLING RISK
Current substitute: system Maven `mvn`
Follow-up task: DH-TOOLING-MAVEN-WRAPPER-REPAIR
```

本轮不修复 wrapper；如修复需要下载 Maven wrapper jar 或外网访问，必须拆独立 tooling 任务。

## 5. Docker / Testcontainers 状态

Docker CLI daemon 当前可用：

```text
docker version: PASS / Docker Desktop 4.80.0 / Engine 29.6.1
docker info: PASS / Server reachable
```

但 Java/Testcontainers 本轮未能通过 named pipe 连接 Docker：

```text
Testcontainers version: 1.20.4
Failure shape: java.nio.file.AccessDeniedException: \\.\pipe\docker_engine
JdbcNonceReplayGuardPersistenceTest: SKIPPED 3
PostgresContainerSmokeTest: SKIPPED 1
Decision: ENVIRONMENT_SKIP / P2 TOOLING_ENV_RISK
```

这说明本机 Docker CLI 可用，但 Testcontainers 没有实际运行 PostgreSQL 容器。skip 不等于 PASS；CI 或后续本机权限修复后必须重新区分记录，不能把历史 skip 追溯为 PASS。

## 6. Review 触发规则

后续不再要求每个普通 implementation batch 单独做长 review。普通批次只要求：

```text
implementation
tests
boundary scan
minimal docs/current sync
commit
```

只保留以下 review 触发条件：

```text
1. migration / 表结构变化
2. API / Controller 变化
3. auth / tenant / HMAC / nonce / source allowlist 变化
4. audit fail-closed / approval / replay 变化
5. stage close / acceptance
6. P0/P1 blocker fix
```

B5 close review 是 stage-qdr-2 的验收节点，允许 review。stage-qdr-3 启动前必须先完成 B5 close review。Agent / LangGraph 仍后置，不因 stage-qdr-2 完成而自动启动。

## 7. 验证记录

```text
git status --short: PASS / clean before write
git branch --show-current: dev
git log --oneline -8: HEAD includes 1e3acf4 feat(qdr): add stage-qdr-2 human approval API
git diff --check: PASS before write
git diff --stat: EMPTY before write
git diff --name-only: EMPTY before write
git ls-files --others --exclude-standard: EMPTY before write
git diff --cached --name-only: EMPTY before write
mvn -ntp -pl dh-domain -am test: BUILD SUCCESS / 136 tests / 0 failures / 0 errors / 0 skipped
mvn -ntp -pl dh-usecase -am test: BUILD SUCCESS / 226 tests / 0 failures / 0 errors / 0 skipped
mvn -ntp -pl dh-infra -am test: BUILD SUCCESS / 53 tests / 0 failures / 0 errors / 3 skipped
mvn -ntp -pl dh-api -am test: BUILD SUCCESS / 77 tests / 0 failures / 0 errors / 0 skipped
mvn -ntp -pl dh-app -am test: BUILD SUCCESS / 54 tests / 0 failures / 0 errors / 1 skipped
mvn -ntp -Pquality validate: BUILD SUCCESS / reactor 19/19 / Checkstyle 0 violations / Spotless passed
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
docker version: PASS / daemon reachable
docker info: PASS / daemon reachable
Forbidden scan: REVIEWED / 1603 hits / no production risk
Skip flags: NOT USED / no -DskipTests / no -DskipITs
```

Forbidden scan 命中分类：

```text
docs prohibition: 1013
test guard: 377
enum constraint or redaction/security code: 150
contract/golden constraint: 50
production candidate review: 13
```

production candidate review 均为已知安全约束或计量字段：`V7__human_approval_packet.sql` 中禁止 `BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER / LIMIT_ORDER / EXECUTE_ORDER` 的 check constraint，以及 `V1__init.sql` 中 `max_tokens` / `tokens` 计量字段。未发现真实下单、撤单、NQ mutation、真实 provider、真实 HTTP 或 LIVE production risk。

## 8. Readiness decision

```text
STAGE_QDR_2_DISCIPLINE_CLOSEOUT: DONE
ALLOW_STAGE_QDR_2_B5_CLOSE_REVIEW: YES
ALLOW_STAGE_QDR_3_START: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步唯一允许动作：

```text
DH-STAGE-QDR-2-B5-CLOSE-REVIEW
```
