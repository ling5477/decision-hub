# DH Stage-QDR-9 B4 Engineering Discipline Reset and Minimal Remediation Plan

## 1. 决策状态

~~~text
task:
DH-STAGE-QDR-9-B4-ENGINEERING-DISCIPLINE-RESET-AND-MINIMAL-REMEDIATION-PLAN

classification:
DOCUMENTATION / GOVERNANCE_RESET / DELIVERY_PATH_REBASELINE

starting HEAD:
2249c312cbc8afd6314751941075e209be06ba01

starting HEAD parent:
aab84e896595bbd8b3f5e99e8b2880ca28f8e7a4

origin/dev / advertised SHA:
b0ff11e4057077ad7e0fe91d691116f069dc744e

starting ahead / behind:
3 / 0

current technical tree:
B3 SAFE BASELINE

Stage-QDR-9 B4:
IMPLEMENTATION REVERTED / REVIEW BLOCKED

technical P1:
OPEN / persistent rate and idempotency identity must bind verified environment;
QDR7 rate audit must record verified environment structurally

technical P2:
OPEN / replay namespace must bind verified environment

actual legacy data:
NOT_PROVABLE

selected minimal remediation path:
M4 / DEPLOYMENT AND LEGACY DATA EVIDENCE REQUIRED BEFORE IMPLEMENTATION

migration reassessment:
R4 / INSUFFICIENT OPERATIONAL EVIDENCE

engineering discipline reset:
PASS / DELIVERY PATH REBASELINED

minimal implementation plan:
BLOCKED BY OPERATIONAL EVIDENCE

local documentation commit:
THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT PUBLISHED

push / tag:
NO / NONE
~~~

本文件是 Stage-QDR-9 B4 当前唯一 canonical remediation plan。任务输入同时给出了较短的
`DH_STAGE_QDR_9_B4_MINIMAL_REMEDIATION_PLAN.md` 名称和本文件的精确交付路径；为避免生成两个
相互竞争的 authority，本轮采用精确交付路径作为唯一主文档，不创建短名副本。

## 2. Primary delivery goal

本阶段当前唯一交付目标是安全关闭两个已验证 finding：

1. P1：
   - persistent rate identity 使用 verified environment；
   - persistent idempotency identity 使用同一个 verified environment；
   - `QDR7_RATE_LIMIT_ADMISSION` 结构化记录该 verified environment。
2. P2：
   - replay namespace 使用同一个 verified environment。

以下事项不是当前 P1/P2 实现的自动前置：

- reference-liveness registry lifecycle；
- retention pipeline；
- 完整 legacy retirement subsystem；
- 永久 tombstone cleanup framework；
- retirement audit subsystem；
- 历史 environment backfill；
- V18 candidate；
- scheduler、generic audit query API、automatic learning 或 B5。

只有经授权的真实部署和数据证据证明需要时，才允许重新评估这些事项。该规则不删除历史设计，也不
改写已经发生的审计记录。

## 3. 范围与停止条件

### 3.1 本任务允许

- 审计当前 authority、Git 历史、CI/CD、Compose、runtime profile、migration schema、
  operational worklog 与可见 GitHub deployment metadata；
- 将 legacy concern 分类为 A/B/C/D；
- 唯一选择 M1/M2/M3/M4 和 R1/R2/R3/R4；
- 更新本计划、`STATUS.md`、Stage-QDR-9 主工单和 `FACTSOURCE_POLICY.md`；
- 执行 docs-only diff 检查和 Maven quality；
- 创建一个本地 docs-only commit。

### 3.2 本任务禁止

- Java、测试、POM、migration、runtime 配置或 workflow 变更；
- 新 scope erratum、新 lifetime blocker、新 migration work order 或新 cleanup design；
- 创建 V16、V17、V18、registry、retention、scheduler、API、B5；
- B4 milestone review retry、B4 publication、push 或 tag；
- 访问未获授权的真实数据库、真实用户数据、凭证或生产写接口。

### 3.3 停止条件

任何未来 implementation 在完成一次性 discovery 后发现 allowlist 外文件，必须：

~~~text
STOP IMPLEMENTATION
RETURN TO DISCOVERY
~~~

不得再通过连续 scope erratum 扩张实施范围。

## 4. Engineering discipline assessment

| 项目 | 事实 | 判定 |
|---|---|---|
| publication sequence | `549ed5a` 在 pre-publication milestone review 未完成时发布；随后因 P1/P2 被普通 revert | governance failure 已由 containment 保留证据并隔离 |
| scope freeze effectiveness | 实施范围从 42/46/47/48 继续演进到 54/60/66 | 早期 freeze 未能形成稳定实施边界 |
| document-to-code ratio | `origin/dev..2249c312` 为 3 个本地 docs commit、23 个文档类文件、4199 insertions、0 production/test/migration lines | 设计增长没有推进 P1/P2 可交付代码 |
| current delivery blockage | 三个 hard lifetime ceiling 无真实数据需求证明，却成为普通修复前置 | delivery path 被推演复杂度阻断 |
| reset verdict | 保留历史设计，撤销其 active acceptance-gate 地位；现实证据优先 | PASS |

`54 / 54`、`60 / 60`、`66 / 66` 继续作为 `HISTORICAL DESIGN EVIDENCE` 可供风险
复核，但状态统一为：

~~~text
NOT ACTIVE IMPLEMENTATION GATE
SUPERSEDED FOR MINIMAL REMEDIATION EXECUTION
~~~

## 5. Actual deployment and data reality audit

### 5.1 证据规则

以下证据只证明 schema 或测试能力，不证明真实 legacy data：

- Flyway migration 或表存在；
- Testcontainers、fixture 或集成测试 insert；
- production class 具备写路径；
- commit 已 push；
- GitHub Actions 执行或通过。

真实 operational data 至少需要可关联的 deployment、running commit、实际写入、保留状态和环境
inventory 证据。涉及真实数据库时还必须有显式只读授权；本任务没有该授权。

### 5.2 已核验证据

| 事实源 | 证据 | 结论 |
|---|---|---|
| Git history | `549ed5a` 为 published implementation；`df921f2` 普通 revert；当前树为 B3 safe baseline | 证明发布和回退，不证明部署 |
| GitHub Actions | commit `549ed5a` 对应 CI run `30283326199`，event=`push`，conclusion=`success` | 仅构建/测试 |
| GitHub Deployments | 以 `sha=549ed5a` 查询返回 0；repository Environments 返回 0 | 该 GitHub 渠道无部署记录；不能排除外部部署 |
| `.github/workflows/ci.yml` | 只有 build/test 与 quality；`permissions: contents: read`；无 deploy job | CI/CD 不包含部署 |
| `ops/README.md` / Compose | Compose 明确为本地 Postgres + Redis | 本地开发设施，不是环境 inventory |
| runtime profiles | prod profile 只提供配置能力；limited runtime 默认 disabled、production disabled、kill switch enabled | 不证明实例曾运行 |
| deployment artifacts | 仓库未发现 Helm/K8s/Terraform/deploy workflow/environment inventory | 无仓库内部署证据 |
| schema | V12 创建 persistent rate/idempotency 表；V13/V14 调整状态合同；当前最高 V15 | schema-only possibility |
| production code | rate/idempotency 当前可写 PostgreSQL；environment 来自 guard configuration；rate audit JSON 不含 environment；replay ref 为 `replay:<requestId>` | 证明 P1/P2 代码现实，不证明真实数据 |
| tests | PostgreSQL/Testcontainers fixture 明确插入和查询相关 rows | test-only evidence |
| operational records | current authority/worklog 记录 production capacity `NOT_PROVEN`，无 running SHA、真实流量、row inventory 或维护窗口证据 | operational evidence 不足 |
| real database | 未提供已授权环境清单、只读连接或脱敏 row inventory | 不访问；真实数据状态不可证明 |

### 5.3 必答问题

~~~text
Was 549ed5a ever deployed to a running DH instance?
NOT_PROVABLE

Did that instance process real external traffic?
NOT_PROVABLE

Did it write persistent rate/idempotency rows?
NOT_PROVABLE

Do any current authorized environments contain legacy rows that must survive the remediation?
NOT_PROVABLE

Is zero-downtime migration a real requirement?
NOT_PROVABLE
~~~

GitHub Deployments 为 0 不能推导“从未部署”；同样，schema 和测试存在也不能推导“必须兼容真实
legacy rows”。

## 6. Data reality classification

分类定义：

- A：有 deployment、运行、写入和保留证据的 confirmed operational data；
- B：仅 Testcontainers、fixture 或本地开发数据库中的 test-only data；
- C：表和代码支持，但没有运行证据的 schema-only possibility；
- D：缺少权限或证据，无法确认真实环境状态。

当前结果：

~~~text
RATE LEGACY DATA CLASS:
D

IDEMPOTENCY LEGACY DATA CLASS:
D

REPLAY LEGACY DATA CLASS:
D

AUDIT LEGACY DATA CLASS:
D
~~~

仓库内同时存在 B 类测试证据和 C 类 schema/code possibility，但没有足够证据把任何 operational
状态从 D 降为 B/C，也没有 A 类证据。

## 7. Minimal remediation path

~~~text
SELECTED MINIMAL REMEDIATION PATH:
M4
~~~

选择理由：

- M1 要求所有相关数据均已证明为 B/C；当前无法证明，故不选择；
- M2 要求 A 类数据和允许维护窗口；两项均无证据；
- M3 要求 A 类数据及明确零停机要求；两项均无证据，禁止选择；
- M4 精确匹配“无环境/数据权限，真实数据与零停机要求不可证明”。

当前拒绝的复杂度：

- 不继续设计 legacy lifetime ceiling；
- 不继续设计 V18、永久 tombstone、retirement audit 或在线 dual-read/write；
- 不将“可能存在”提升为“必须迁移”；
- 不以 registry/retention 代替 P1/P2 最小修复。

解除 M4 至少需要一份经过授权、可审查且脱敏的 evidence packet：

1. 当前允许环境 inventory 和 owner；
2. 每个环境实际运行过的 commit SHA 与时间窗；
3. `549ed5a` 是否启动并接收外部流量；
4. rate/idempotency/replay/audit 相关表的存在性、row count、最早/最晚时间和可保留要求；
5. writer 是否可停、允许维护窗口与 in-flight drain 条件；
6. 明确的 zero-downtime 要求或明确“不要求”；
7. 证据采集命令、执行人/系统、时间和脱敏结果。

不得在文档或日志中记录数据库密码、连接 secret、token 或原始用户数据。

## 8. Migration reassessment

~~~text
Is reference-liveness registry required to fix P1/P2?
NO

Is V16 required before audit environment storage?
NO

Is V18 required for confirmed current data?
NO / NO CONFIRMED CURRENT DATA REQUIRES IT

Can P1/P2 be fixed independently from registry/retention?
YES

SELECTED MIGRATION REASSESSMENT:
R4 / INSUFFICIENT EVIDENCE / IMPLEMENTATION BLOCKED
~~~

依据：

- V12 已使 persistent rate/idempotency schema 具有 environment 字段；P1 的核心缺口是 environment
  authority 与同一 verified scope 的传播，不是 reference-liveness registry。
- 当前 V5 `dh_decision_audit_event` 没有独立 environment column；未来最小 discovery 必须判断是否
  用一个 additive migration 为新 `QDR7_RATE_LIMIT_ADMISSION` 写入增加结构化 environment。
  该 schema 需求不依赖 registry。
- replay namespace 是应用身份编码问题，不依赖 registry 或 retention。
- V16/V17/V18 均未创建。此前 `V16 registry -> V17 audit -> V18 legacy` 序列保留为历史设计证据，
  但对最小修复执行标记为 `SUPERSEDED FOR MINIMAL REMEDIATION EXECUTION`。
- “V18 当前不需要”只表示没有 confirmed current data 支持它；如果 evidence packet 后续证明 A 类
  数据存在，必须重新选择 M2/M3 并重新评估一次性 migration。

本任务不预占新的 migration version。解除 M4 并完成一次性 discovery 后，`MIGRATION_ALLOWLIST`
才能冻结 exact filename、DDL、兼容/回滚验证和是否需要 backfill。

## 9. Future minimal implementation boundary

如果 evidence packet 使路径转为 M1 或 M2，未来 implementation 只允许围绕：

1. canonical `FeedbackEnvironment` 与 `FeedbackExecutionScope`；
2. signed environment HMAC binding；
3. verified `AuthContext` environment；
4. tenant/source/environment authorization；
5. environment-bound rate identity；
6. environment-bound idempotency identity；
7. environment-bound replay namespace；
8. structured QDR7 rate-audit environment；
9. DEV/TEST isolation tests；
10. root fail-closed tests；
11. PostgreSQL/Testcontainers regression；
12. 必要 architecture guards。

潜在 schema 需求只限 QDR7 rate-audit 的 structured environment storage；是否需要 migration、
backfill 和 exact constraint 由 evidence + discovery 决定。

明确排除：

- reference-liveness registry；
- retention、cleanup、scheduler；
- legacy retirement subsystem；
- historical backfill（除非 A 类数据证据明确要求）；
- V18；
- generic audit query API；
- API 契约扩张；
- automatic learning、B5、真实 Provider/NQ/HTTP、Paper 或 LIVE。

## 10. One-time discovery contract

M4 解除后，implementation 前必须一次性审计：

- production call graph；
- all constructors；
- all repositories；
- all schema paths；
- all Spring wiring；
- all mandatory tests；
- all architecture guards；
- all compatibility fixtures；
- all Maven dependencies。

discovery 必须一次性输出：

~~~text
MINIMAL_IMPLEMENTATION_WRITE_ALLOWLIST
MINIMAL_TEST_ALLOWLIST
MIGRATION_ALLOWLIST
FACTSOURCE_UPDATE_ALLOWLIST
~~~

在这些 exact allowlist 冻结前：

~~~text
ALLOW_MINIMAL_IMPLEMENTATION:
NO
~~~

## 11. Authority model simplification

~~~text
Canonical active authority:
docs/current/STATUS.md

Canonical implementation plan:
docs/current/DH_STAGE_QDR_9_B4_ENGINEERING_DISCIPLINE_RESET_AND_MINIMAL_REMEDIATION_PLAN.md
~~~

十二个 terminal factsources 只在以下事件同步：

1. containment；
2. implementation milestone acceptance；
3. publication；
4. authority close；
5. stage close。

scope design、investigation 和 blocker 子任务只更新：

- `docs/current/STATUS.md`；
- `docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md`；
- 当前唯一目标设计/计划文档。

历史 identity/replay design、audit environment design、legacy identity design 与 scope errata 不删除、
不改写，统一视为：

~~~text
HISTORICAL DESIGN EVIDENCE
NOT ACTIVE IMPLEMENTATION GATE
~~~

## 12. Validation and readiness

~~~text
full regression:
NOT_RERUN / DOCUMENTATION-ONLY GOVERNANCE RESET

reused containment baseline:
1228 tests / 0 failures / 0 errors / 0 skipped

quality:
PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS

Java / test / POM / migration diff:
MUST REMAIN 0

V16 / V17 / V18 files:
MUST REMAIN 0

ENGINEERING_DISCIPLINE_RESET:
PASS

ACTUAL_LEGACY_DATA:
NOT_PROVABLE

MINIMAL_REMEDIATION_PATH:
M4

PREVIOUS_54_60_66_SCOPES:
HISTORICAL

REGISTRY_REQUIRED_FOR_P1_P2:
NO

V18_REQUIRED:
NO / NO CONFIRMED CURRENT DATA

MINIMAL_IMPLEMENTATION_PLAN:
BLOCKED

AUTHORITY_MODEL_SIMPLIFIED:
PASS

ALLOW_MINIMAL_IMPLEMENTATION_DISCOVERY:
NO / EVIDENCE BLOCKER FIRST

ALLOW_LEGACY_LIFETIME_BLOCKER:
NO

ALLOW_V16_IMPLEMENTATION:
NO

ALLOW_B4_REVIEW_PUBLICATION:
NO

ALLOW_B5:
NO

PRODUCTION_CAPACITY:
NOT_PROVEN
~~~

## 13. Next concrete action

~~~text
DH-STAGE-QDR-9-B4-DEPLOYMENT-AND-LEGACY-DATA-EVIDENCE-BLOCKER
~~~

该任务只能取得或记录经授权的部署与脱敏数据证据，不实施代码、migration、registry、retention、
review retry、publication 或 B5。
