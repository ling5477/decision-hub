# DH Stage-QDR-9 B5 Stage Final Close Plan

## 0. Planning authority

~~~text
Task:
DH-STAGE-QDR-9-B5-STAGE-FINAL-CLOSE-PLAN

Task classification:
PLANNING_ONLY
+ STAGE_FINAL_CLOSE_GOVERNANCE_PLAN
+ FACTSOURCE_SYNC_PLAN
+ ARCHIVE_PACKET_PLAN
+ TAG_CLOSE_SEQUENCE_FREEZE
+ POST_TAG_CLEANUP_PLAN
+ DEFERRED_CAPABILITY_HANDOFF
+ CAPACITY_CLASSIFICATION
+ DOCS_ONLY_CHANGE
+ NO_TECHNICAL_IMPLEMENTATION
+ NO_API_CHANGE
+ NO_MIGRATION_CHANGE
+ NO_REPOSITORY_CHANGE
+ NO_CAPACITY_EXECUTION
+ NO_SERVER_DEPLOYMENT

Planning baseline:
c7f940c0c48900a0cfb7eac86aac745c8006629c

Accepted CI:
30633947829 / PASS / exact head SHA matched

Stage-QDR-9 B1 / B2 / B3 / B4:
CLOSED / ACCEPTED / PUBLISHED

Active P0 / P1:
0 / 0

Stage-QDR-9 functional close eligibility:
YES

B5_TYPE:
FINAL_CLOSE_BATCH

B5_TECHNICAL_IMPLEMENTATION_REQUIRED:
NO

B5_GOVERNANCE_CLOSE_EXECUTION_REQUIRED:
YES

B5 final-close execution:
NOT EXECUTED

Stage-QDR-9 governance close:
NOT COMPLETED

Formal capacity:
NOT_EXECUTED / DEFERRED

Production capacity:
NOT_PROVEN

Production ready:
NO

Planning scope invariants:
PASS / 3 OF 3

Terminal factsource inventory at planning baseline:
12 TOTAL / 10 STALE / 2 CURRENT

Planning document commit:
THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT PUBLISHED

Next task:
DH-STAGE-QDR-9-B5-STAGE-FINAL-CLOSE

ALLOW_B5_GOVERNANCE_CLOSE_EXECUTION:
YES / NEXT TASK ONLY / CONSOLIDATED FOUR-PHASE EXECUTION

ALLOW_STAGE_QDR_9_CLOSE_NOW / ALLOW_ARCHIVE_CREATION_NOW / ALLOW_TAG_NOW / ALLOW_POST_TAG_PRUNING_NOW:
NO / NO / NO / NO

ALLOW_NEXT_STAGE_PLAN / ALLOW_V16_IMPLEMENTATION / ALLOW_RETENTION_IMPLEMENTATION / ALLOW_CAPACITY_EXECUTION / ALLOW_SERVER_DEPLOYMENT:
NO / NO / NO / NO / NO
~~~

本文件只冻结 B5 的治理执行方案，不执行 Stage close、archive、tag、pruning、capacity、部署或
技术变更。`ALLOW_B5_GOVERNANCE_CLOSE_EXECUTION` 只授权下一精确任务按本文件的四个内部阶段
执行；它不把任何内部阶段提前到本 planning task。

## 1. 目标、边界与完成定义

### 1.1 目标

1. 将 B5 固定为一个治理型 `FINAL_CLOSE_BATCH`，而不是第五个技术实现 batch。
2. 冻结 12 个 terminal factsources 的一次性同步矩阵和 `CURRENT_CONFLICT = 0` close gate。
3. 冻结 self-contained Stage-QDR-9 archive packet、31 份 source document copy 与完整性证据。
4. 冻结 close commit、exact-SHA CI、annotated tag、remote tag verification 与 post-tag cleanup 顺序。
5. 将 capacity、reference-liveness、retention 与 V16/V17/V18 明确移交给未来独立 capability planning。
6. 只允许一个 consolidated final-close 下一任务，禁止治理流程反复拆分成 docs-fix/review 循环。

### 1.2 B5 不包含

~~~text
Java production implementation
test implementation
migration
API / Controller / OpenAPI
Repository
retention
reference-liveness
V16 / V17 / V18
legacy retirement
capacity benchmark
server deployment
real HTTP / real Provider / NQ runtime
Agent / LangGraph / Paper / LIVE
automatic learning
~~~

### 1.3 B5 只负责

~~~text
stage acceptance
terminal factsource synchronization
archive packet
source-copy manifest
close evidence
close commit
exact-SHA CI
annotated tag
remote tag verification
post-tag current cleanup
next-stage planning authorization
~~~

### 1.4 Stage-close 完成定义

只有以下条件全部成立，Stage-QDR-9 才可记录为治理关闭完成：

~~~text
B1-B4 CLOSED / ACCEPTED / PUBLISHED
active P0/P1 = 0/0
technical baseline and accepted CI revalidated
close diff is strictly docs-only
12 terminal factsources synchronized / CURRENT_CONFLICT = 0
self-contained archive packet present
31 source documents copied and integrity-verified
close report PASS
close commit published by ordinary fast-forward
close commit exact-SHA CI PASS
annotated tag dh-stage-qdr-9-close created on the close commit
local and remote tag target verified equal
post-tag current-source cleanup committed and published
origin/dev aligned
worktree clean / staged empty
current authority consistent
~~~

`FORMAL_CAPACITY = NOT_EXECUTED / DEFERRED` 不阻断 Stage-QDR-9 functional close，但阻断
`PRODUCTION_READY = YES`。任何普通 CI 或 1243 tests 都不得改写为 formal capacity acceptance。

## 2. 已核验代码与治理现实

### 2.1 原始 Stage 目标和 B5 定义

原始 [Stage-QDR-9 plan](DH_STAGE_QDR_9_PLAN.md) 将主线冻结为
`STRUCTURED_FEEDBACK_ATTRIBUTION_PERSISTENCE + HISTORICAL_EVIDENCE_READ_MODEL`；原始
[implementation work order](DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md) 将 B5 定义为 final
close，并要求 tests、PostgreSQL/Testcontainers、architecture、quality、factsource、archive、tag、
pruning 与 machine dependency scan。

当前 B1-B4 已完成技术实现、review、publication 与 exact-SHA CI。因 B5 不再需要新增技术实现，
本计划把原 B5 中的技术验证要求具体化为“先复核已接受技术证据，再验证 close commit 严格
docs-only，并在 close commit 发布后运行 exact-SHA CI”，不在本 planning task 重跑 full tests。

### 2.2 B1-B4 实际状态

| Batch | 状态 | 主要接受证据 | B5 处理 |
|---|---|---|---|
| B1 | CLOSED / ACCEPTED / PUBLISHED | V15 schema、scope erratum、milestone review PASS；1208 tests；PostgreSQL 17.10 | 归档为 `HISTORICAL_EVIDENCE`，不重开 migration review |
| B2 | CLOSED / ACCEPTED / PUBLISHED | JDBC persistence、事务/幂等、CI 30187110533；1220 tests | 归档 acceptance，不修改 Repository |
| B3 | CLOSED / ACCEPTED / PUBLISHED | internal-only/read-only historical evidence read model、CI 30190532421；1228 tests | 保持 internal-only/read-only；两项 P2 只作 backlog |
| B4 | CLOSED / ACCEPTED / PUBLISHED | minimal environment remediation、authority rebaseline/final close；当前技术 SHA 与 CI 30633947829 | 归档 discipline/remediation 链；deferred capability 不恢复 |

### 2.3 当前有效技术证据

~~~text
Technical implementation SHA:
c7f940c0c48900a0cfb7eac86aac745c8006629c

Accepted CI:
30633947829 / PASS

Accepted tests:
1243 / 0 failures / 0 errors / 0 skipped

PostgreSQL:
17.10 / Testcontainers real execution / mandatory reports not skipped

Architecture:
ArchitectureTest PASS / StageQdr9FeedbackArchitectureTest PASS

Quality:
19 of 19 reactor / Checkstyle 0 / Spotless PASS
~~~

CI 元数据已证明 run `30633947829` 的 `headSha` 精确等于 `c7f940c...`，`Quality` 与
`build & test (Testcontainers / Docker)` 两个 job 均为 `success`。这些证据是功能回归证据，
不是 capacity acceptance。

### 2.4 活跃技术缺口和 deferred 状态

~~~text
Active P0/P1: 0/0
Reference-liveness: DEFERRED / FUTURE INDEPENDENT CAPABILITY
Retention: DEFERRED / FUTURE INDEPENDENT CAPABILITY
V16: HISTORICAL CANDIDATE / NOT AUTHORIZED
V17/V18: SUPERSEDED / NOT ACTIVE
Legacy retirement: NOT AUTHORIZED
Formal capacity: NOT_EXECUTED / DEFERRED
Production capacity: NOT_PROVEN
Server deployment: NOT PERFORMED / NOT AUTHORIZED
~~~

## 3. Scope contract

### 3.1 本 planning task

~~~text
READ_SCOPE:
  task attachment
  README.md / AGENTS.md / CLAUDE.md
  12 terminal factsources
  30 existing docs/current/DH_STAGE_QDR_9*.md source documents
  B1-B4 close, acceptance, review and evidence documents
  docs/gates/stage-qdr-7 and stage-qdr-8 file inventories
  git refs, commit c7f940c..., tag and archive-path state
  GitHub Actions run 30633947829 metadata and relevant logs
  pom.xml, selected module src, config/contracts/.github/scripts for validation and dependency scan

WRITE_ALLOWLIST:
  docs/current/DH_STAGE_QDR_9_B5_STAGE_FINAL_CLOSE_PLAN.md
  docs/current/STATUS.md
  docs/current/WORK_ORDER.md
  docs/current/WORKLOG.md
  docs/current/ROADMAP.md
  docs/current/TESTING.md
  docs/current/README.md

VALIDATION_SCOPE:
  WRITE_ALLOWLIST
  git worktree/staged/tag/archive-path checks
  pom.xml and selected module/config/contract/workflow/script boundary checks

FIXABLE_BLOCKER_SCOPE:
  WRITE_ALLOWLIST

CANONICAL_AUTHORITY_SCAN_SCOPE:
  docs/current/STATUS.md
  docs/current/WORK_ORDER.md
  docs/current/WORKLOG.md
  docs/current/ROADMAP.md
  docs/current/TESTING.md
  docs/current/README.md

TERMINAL_FACTSOURCE_INVENTORY_SCOPE:
  the exact 12 paths in section 4
  READ_ONLY inventory in this planning task
~~~

~~~text
VALIDATION_SCOPE subset READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE subset WRITE_ALLOWLIST: PASS
CANONICAL_AUTHORITY_SCAN_SCOPE subset WRITE_ALLOWLIST: PASS
TOTAL: 3 OF 3 PASS
~~~

`TERMINAL_FACTSOURCE_INVENTORY_SCOPE` 不受本轮 write allowlist 约束，也不设置
`CURRENT_CONFLICT = 0` planning gate。B5 execution 必须重新盘点并在 tag 前归零。

### 3.2 下一 B5 execution 的精确边界

下一任务必须在写前重新冻结：

~~~text
12 terminal factsources
docs/gates/stage-qdr-9/** new archive packet
31 exact docs/current/DH_STAGE_QDR_9*.md source paths
post-tag deletion/move set for those 31 process/source docs
root/current archive and entry indexes
no production/test/migration/API/Repository/contracts writes
~~~

任何技术文件进入 diff 时立即停止为 `DH-STAGE-QDR-9-TECHNICAL-SCOPE-REOPEN-REVIEW`。

## 4. Terminal factsource synchronization matrix

本表记录 `c7f940c...` planning preflight 的实际顶部 authority。扫描得到实际 stale 数为 `10`，
不是历史记忆或硬编码的 `9`。本 planning task 会最小更新 allowlisted current 文档，但 B5 必须在
execution 起点重新扫描 12 个路径并记录新的实际 stale 数。

| 文件 | Authority role | 当前状态（planning baseline） | 是否 stale | B5 同步动作 | tag 后动作 |
|---|---|---|---:|---|---|
| `AGENTS.md` | `EXECUTION_GUIDANCE` | legacy persistent identity blocker | 是 | 写入 Stage close boundary、deferred/capacity 口径与下一阶段门禁 | 保留全局执行规范和简洁 Stage close 摘要 |
| `CLAUDE.md` | `EXECUTION_GUIDANCE` | legacy persistent identity blocker | 是 | 与 primary authority 同步，不保留旧 blocker 为顶部口径 | 保留执行指导和 archive/tag 指针 |
| `README.md` | `ENTRY_INDEX` | legacy persistent identity blocker | 是 | 更新 Stage-QDR-9 close 摘要、archive/tag 指针 | 保留 concise 入口，不承载完整历史 |
| `docs/current/README.md` | `ENTRY_INDEX` | legacy persistent identity blocker | 是 | 索引 B5 close report 和 archive packet | pruning 后移除 Stage-QDR-9 process-doc 链接，保留 archive 入口 |
| `docs/current/STATUS.md` | `PRIMARY_AUTHORITY` | B4 CLOSED / B5 NOT AUTHORIZED | 否 | 写入 Stage-QDR-9 CLOSED/ARCHIVED/TAGGED 与 cleanup 状态 | 保留 current 阶段摘要、capacity/deferred 限制和 next-stage gate |
| `docs/current/WORK_ORDER.md` | `PRIMARY_AUTHORITY` | B4 CLOSED / next-gate decision | 否 | 写入 B5 四阶段完成记录和唯一 next action | cleanup 后仅保留下一任务与禁止项 |
| `docs/current/ROADMAP.md` | `EXECUTION_GUIDANCE` | legacy persistent identity blocker | 是 | 关闭 Stage-QDR-9 路线，记录 deferred handoff | 保留 closed summary 与 future independent capability route |
| `docs/current/TESTING.md` | `HISTORICAL_SOURCE` | legacy persistent identity blocker | 是 | 记录 reused evidence、close exact-SHA CI 与非 capacity 声明 | 保留验证记录；不删除共享 testing 文档 |
| `docs/current/WORKLOG.md` | `HISTORICAL_SOURCE` | legacy persistent identity blocker | 是 | 追加 B5 phase、commit、CI、tag、cleanup 实际结果 | 保留 append-only 记录；不把计划写成已执行 |
| `docs/current/CODEX_PROJECT_INSTRUCTIONS.md` | `EXECUTION_GUIDANCE` | legacy persistent identity blocker | 是 | 同步 Stage close/deferred/next-stage prerequisites | 保留全局指导，移除 active QDR-9 process pointer |
| `docs/current/FACTSOURCE_POLICY.md` | `POLICY_AUTHORITY` | legacy persistent identity blocker | 是 | 记录 12/12 final sync、archive/pruning 后 authority hierarchy | 保留 policy；不得随 Stage process docs 删除 |
| `docs/current/ARCHIVE_INDEX.md` | `HISTORICAL_SOURCE` | legacy persistent identity blocker | 是 | 登记 archive commit、close tag target、cleanup commit | 保留 Stage-QDR-9 immutable archive 指针 |

B5 tag 前强制条件：

~~~text
terminal factsources = 12 / 12 inventoried
one current Stage-close authority
CURRENT_CONFLICT = 0
historical blocks remain explicitly historical
primary authority wins over indexes/guidance/history
~~~

## 5. Archive packet freeze

### 5.1 目标目录与顶层文件

目标目录固定为 `docs/gates/stage-qdr-9/`。Phase 2 必须一次性形成以下 self-contained packet：

| Archive path | 内容 |
|---|---|
| `README.md` | Stage 目标、边界、batch 状态、close/tag/cleanup state 与入口 |
| `PLAN.md` | 原始 Stage-QDR-9 plan 的冻结副本/规范化入口 |
| `IMPLEMENTATION_WORK_ORDER.md` | 原始 implementation work order 的冻结副本/规范化入口 |
| `BATCH_SUMMARY.md` | B1-B4 范围、提交、review、publication 与结果 |
| `VALIDATION_EVIDENCE.md` | targeted/full/architecture/quality/PostgreSQL 证据与 reused/rerun 标记 |
| `TECHNICAL_EVIDENCE_SUMMARY.md` | `c7f940c...` 技术基线、1243/0/0/0、PostgreSQL 17.10、安全边界 |
| `CI_EVIDENCE_REFERENCE.md` | CI 30633947829 与 close commit exact-SHA CI 的 run/head/job/url 证据 |
| `FINAL_CLOSE_REVIEW.md` | B5 final-close report、findings、capacity/deferred 分类与 PASS/BLOCKED 决策 |
| `DISCIPLINE_REPAIR.md` | B4 containment、owner-attested minimal remediation、rebaseline/final-close 证据链 |
| `ARCHIVE_CLOSE.md` | archive completeness、close commit/tag 前条件与 tag state |
| `STATUS_SNAPSHOT.md` | tag 前 12/12 terminal factsources 的一致状态快照 |
| `SOURCE_MANIFEST.md` | 31 份 source document 的逐文件路径、分类、commit、copy 与 hash |
| `MANIFEST.md` | packet 顶层文件用途、required/optional、生成结果 |
| `SHA256SUMS` | packet 与 source copies 的 SHA-256 完整性证据 |
| `source-documents/*.md` | 第 5.2 节 31 份 current source 的完整可搜索副本 |

只创建单个 `README.md` 不满足 archive-before-tag；任何 required packet file 缺失均阻断 tag。

### 5.2 Source-copy 精确集合与分类

本 planning baseline 有 30 份 `docs/current/DH_STAGE_QDR_9*.md`；加本文件后，B5 起点预期为
31 份。B5 必须重新用 `rg --files docs/current | rg 'DH_STAGE_QDR_9'` 验证实际集合；若数量或路径
变化，先解释差异并更新 manifest，禁止静默遗漏。

| Source path（均位于 `docs/current/`） | Document classification |
|---|---|
| `DH_STAGE_QDR_9_PLAN.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B1_SCOPE_ERRATUM.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B1_MILESTONE_REVIEW.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B2_MILESTONE_REVIEW.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B3_MILESTONE_REVIEW.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_ENGINEERING_DISCIPLINE_RESET_AND_MINIMAL_REMEDIATION_PLAN.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_MINIMAL_P1_P2_IMPLEMENTATION_DISCOVERY.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_BLOCKED_REMOTE_COMMIT_CONTAINMENT.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_UPSTREAM_CONTRACT_REMOTE_CONTAINMENT.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_UPSTREAM_CONTRACT_SCOPE_ERRATUM.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_UPSTREAM_MODULE_DEPENDENCY_SCOPE_ERRATUM.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_PERSISTENT_GUARD_FIXTURE_SCOPE_ERRATUM.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_CONTRACT_DESIGN.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SCOPE_WORK_ORDER.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SOURCE_RESOLUTION.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SOURCE_IMPLEMENTATION_WORK_ORDER.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_UPSTREAM_AUTHORITY_DECISION.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_UPSTREAM_IMPLEMENTATION_WORK_ORDER.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_IDENTITY_AND_REPLAY_NAMESPACE_DESIGN.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_IDENTITY_AND_REPLAY_NAMESPACE_WORK_ORDER.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_IDENTITY_AND_REPLAY_NAMESPACE_SCOPE_ERRATUM.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_STORAGE_DESIGN.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_FORWARD_MIGRATION_WORK_ORDER.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_SCOPE_ERRATUM.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_LEGACY_PERSISTENT_IDENTITY_DESIGN.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_LEGACY_PERSISTENT_IDENTITY_WORK_ORDER.md` | `SUPERSEDED_DESIGN` |
| `DH_STAGE_QDR_9_B4_LEGACY_PERSISTENT_IDENTITY_SCOPE_ERRATUM.md` | `HISTORICAL_EVIDENCE` |
| `DH_STAGE_QDR_9_B4_REFERENCE_LIVENESS_STATE_MODEL_DESIGN.md` | `DEFERRED_CAPABILITY` |
| `DH_STAGE_QDR_9_B4_REFERENCE_LIVENESS_FORWARD_MIGRATION_WORK_ORDER.md` | `DEFERRED_CAPABILITY` |
| `DH_STAGE_QDR_9_B5_STAGE_FINAL_CLOSE_PLAN.md` | `HISTORICAL_EVIDENCE` |

`DEFERRED_CAPABILITY`、`SUPERSEDED_DESIGN` 与 `HISTORICAL_EVIDENCE` 仅是 archive classification；
它们不能成为 current requirement、implementation authorization 或 production-readiness 证据。

### 5.3 Source manifest 字段和完整性

`SOURCE_MANIFEST.md` 每一行必须至少包含：

~~~text
source path
archive path
source commit SHA
document classification
copy status
content hash or equivalent integrity evidence
~~~

执行规则：

1. `source commit SHA` 使用 close baseline 上 `git log -1 --format=%H -- <source>` 的实际值。
2. `archive path` 固定为 `docs/gates/stage-qdr-9/source-documents/<filename>`。
3. `copy status` 只能在文件存在且 byte/content equivalence 验证后写 `COPIED / VERIFIED`。
4. 每个 source 和 archive copy 都计算 SHA-256；hash 不一致立即阻断。
5. source manifest、packet manifest 与 `SHA256SUMS` 三者集合必须一致，不能用 aggregate summary 替代原始 source。

## 6. 技术证据复用和重新验证规则

### 6.1 本 planning task

~~~text
Full tests: NOT_RERUN
PostgreSQL/Testcontainers: NOT_RERUN
Reused evidence: CI 30633947829 / 1243 / 0 / 0 / 0 / PostgreSQL 17.10
Planning validation: docs-only diff + Maven quality only
~~~

### 6.2 B5 execution

1. Phase 1 重新验证 technical SHA 为 `c7f940c...`，且 accepted CI 30633947829 的 head SHA、
   conclusion、两个 jobs 和 PostgreSQL/Testcontainers evidence 仍可核验。
2. Phase 2/3 的 archive、factsource、close report diff 必须严格 docs-only；任何 production、test、
   migration、API、Controller、Repository、contracts 或 workflow diff 立即停止。
3. close commit 发布后必须等待该 commit 的 exact-SHA CI；CI 未成功不得创建 tag。
4. 当前 `.github/workflows/ci.yml` 对 `dev` push 无 `paths`/`paths-ignore`，因此预期 close docs commit
   会触发完整 CI。若未来 workflow 变更导致 docs path 不触发，必须在 close report 中证明：
   - close commit 相对 `c7f940c...` 严格 docs-only；
   - workflow 的 no-trigger 条件真实存在；
   - `c7f940c...` + CI 30633947829 仍是代码基线；
   - no-trigger 不能写成“close commit CI PASS”。
5. 若后续任务没有对 `DOCS_ONLY_NO_TRIGGER_EQUIVALENCE` 作显式接受，no-trigger 时保持 tag
   `BLOCKED`；不得用普通 CI 或历史 tests 冒充 close commit exact-SHA PASS。
6. exact-SHA CI 的普通回归结果仍不是 formal capacity acceptance。

## 7. Consolidated final-close execution sequence

下一任务只能是 `DH-STAGE-QDR-9-B5-STAGE-FINAL-CLOSE`，并在同一任务内按四个内部阶段执行。
每个阶段是不可跳过的 checkpoint，不是四个独立 review task。

### Phase 1 — Authority and close evidence revalidation

~~~text
revalidate branch/HEAD/origin/advertised SHA and clean baseline
verify local/remote close tag absent and archive path absent
verify B1-B4 CLOSED / ACCEPTED / PUBLISHED
verify active P0/P1 = 0/0
verify c7f940c... and CI 30633947829
inventory 12 terminal factsources and actual stale count
inventory 31 source documents and machine dependencies
freeze exact execution read/write/validation/deletion scope
~~~

### Phase 2 — Factsource synchronization + archive packet + source manifest + close report

~~~text
synchronize 12 terminal factsources to one Stage-close authority
require CURRENT_CONFLICT = 0
create full docs/gates/stage-qdr-9 packet
copy and hash-verify all 31 source documents
write manifests, technical/CI evidence and status snapshot
write final-close review/report with capacity/deferred classification
verify docs-only diff and archive completeness
~~~

### Phase 3 — Close commit publication + exact-SHA CI + annotated tag + remote verification

~~~text
create one Stage close docs commit containing the complete archive packet
ordinary fast-forward push only
verify origin/dev equals close commit
wait for close commit exact-SHA CI PASS
create annotated tag dh-stage-qdr-9-close on the close commit
push that exact tag only
verify local tag object, peeled target and remote peeled target are equal
~~~

### Phase 4 — Post-tag current-source pruning + cleanup publication + alignment

~~~text
revalidate tag local+remote before deletion
verify source copies and hashes before pruning
remove/move 31 Stage-QDR-9 process/source docs from docs/current
keep complete copies in docs/gates/stage-qdr-9/source-documents
update ARCHIVE_INDEX and root/current README entries
retain STATUS/WORK_ORDER/ROADMAP and other shared authority summaries
do not delete shared policy/testing/governance files
run machine dependency and residue scans
create and ordinary-fast-forward publish one cleanup commit
verify origin/dev aligned, worktree clean, staged empty and current authority consistent
~~~

禁止拆成 `docs fix -> standalone review -> archive fix -> standalone review -> tag review -> cleanup review`。
仅新 P0/P1、代码变更、migration/API、Repository 或安全边界变化允许停止并转独立 review。

## 8. Close commit and tag rules

### 8.1 Close commit

建议 close commit message：

~~~text
docs(qdr): close and archive stage-qdr-9
~~~

close commit 必须包含完整 archive packet、12/12 factsource sync、source copies/manifests、close report，
不得包含 post-tag pruning。它只能 ordinary fast-forward 发布，不允许 amend/rebase/force push/history rewrite。

### 8.2 Annotated tag

~~~text
Tag:
dh-stage-qdr-9-close

Type:
annotated

Message:
Close DH Stage-QDR-9 structured feedback persistence and historical evidence baseline

Target:
the Stage close commit that contains the complete archive packet and passed exact-SHA CI
~~~

强制规则：

- tag 不得指向 `c7f940c...`，除非完整 close packet 已存在于该提交；当前事实为不存在。
- tag 不得指向未通过 exact-SHA CI 的提交。
- tag 不得在 archive packet 完成、31 source copies verified、`CURRENT_CONFLICT = 0` 前创建。
- tag 不得移动、覆盖或删除；若 local/remote tag 意外出现且 target 不匹配，立即停止。
- `git rev-parse <tag>^{}`、`git ls-remote --tags origin` peeled SHA 与 close commit 三者必须一致。

## 9. Post-tag cleanup plan

### 9.1 删除/移动范围

Phase 4 的 candidate set 是第 5.2 节 31 份 `docs/current/DH_STAGE_QDR_9*.md`。删除前必须确认：

1. 每个 candidate 已在 tag 指向的 close commit 中存在完整 archive copy。
2. `SOURCE_MANIFEST.md`、`SHA256SUMS` 和逐文件 hash 全部验证通过。
3. `config`、`contracts`、`src/main`、`src/test`、`.github`、`scripts`、`deploy` 对 candidate 的机器依赖为 0；
   planning baseline 扫描为 0，但执行时必须重跑。
4. 删除目标的解析路径全部位于 `docs/current` 且精确匹配 manifest，不使用宽泛递归删除。

### 9.2 必须保留

~~~text
docs/gates/stage-qdr-9 complete packet and source copies
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
root/current README indexes
AGENTS.md / CLAUDE.md
all shared policy, testing and governance files
~~~

### 9.3 Cleanup acceptance

~~~text
rg --files docs/current | rg 'DH_STAGE_QDR_9' => 0 process/source residues
archive source count and hashes remain complete
archive index points to close commit, tag and cleanup commit
root/current README points to immutable archive, not removed current docs
tag remains on pre-pruning close commit
cleanup commit published by ordinary fast-forward
origin/dev = HEAD
worktree clean / staged empty
CURRENT_FACTSOURCE_CONSISTENCY = PASS / 12 OF 12 / 0 CONFLICTS
~~~

## 10. Capacity and deferred-capability handoff

### 10.1 Capacity classification

~~~text
FORMAL_CAPACITY:
NOT_EXECUTED / DEFERRED

PRODUCTION_CAPACITY:
NOT_PROVEN

STAGE_QDR_9_FUNCTIONAL_CLOSE:
DOES NOT DEPEND ON FORMAL CAPACITY

PRODUCTION_READY:
NO
~~~

未来 capacity task 必须作为独立 planning-first capability，重新冻结环境、规模、阈值、数据集、
hardware/runtime、证据和回滚；不得用 Stage-QDR-9 regression、Testcontainers 或普通 CI 代替。

### 10.2 Deferred capability handoff

以下能力只能由未来独立 planning 重新授权：

| Capability | Stage close classification | B5 action |
|---|---|---|
| reference-liveness | `DEFERRED_CAPABILITY` | 归档历史设计，不实施 registry/lifecycle |
| retention | `DEFERRED_CAPABILITY` | 不实现 cleanup/delete/scheduler，不恢复旧默认 |
| V16 | `HISTORICAL_CANDIDATE` | 不创建、不执行、不作为 close gate |
| V17/V18 | `SUPERSEDED_DESIGN` | 只作历史证据，不恢复 sequence |
| legacy retirement | `NOT_AUTHORIZED` | 不迁移、不 backfill、不物理删除 |
| server capacity | `NOT_EXECUTED / DEFERRED` | 不运行 benchmark，不声明 production capacity |
| production deployment | `NOT_AUTHORIZED` | 不部署、不访问真实持久数据 |

## 11. Review triggers and stop conditions

只有以下真实变化触发停止并另行 review：

~~~text
new active P0/P1
any production or test code diff
any migration or schema change
any API / Controller / OpenAPI change
any Repository or transaction semantic change
any security / tenant / environment boundary change
accepted technical SHA or CI cannot be verified
archive/source hash mismatch
terminal CURRENT_CONFLICT cannot be reduced to 0
tag already exists or points to an unexpected commit
non-fast-forward publication would be required
~~~

路由：

- current authority 无法对齐：`DH-STAGE-QDR-9-B5-FINAL-CLOSE-AUTHORITY-BLOCKER`。
- 新 active technical gap：`DH-STAGE-QDR-9-TECHNICAL-SCOPE-REOPEN-REVIEW`。
- 其他治理错误在 consolidated B5 的 exact docs scope 内最小修复，不创建循环 review。

## 12. Next-stage authorization condition

只有以下全部完成后才可授权下一阶段 planning：

~~~text
archive packet present and self-contained
close tag local + remote verified
post-tag cleanup committed and published
origin/dev aligned
worktree clean / staged empty
current authority consistent / 12 of 12 / 0 conflicts
no Stage-QDR-9 process/source residue in docs/current
capacity and deferred capability wording preserved
~~~

本 planning task 结束时上述条件尚未完成，因此：

~~~text
ALLOW_NEXT_STAGE_PLAN: NO
ALLOW_STAGE_QDR_9_CLOSE_NOW: NO
ALLOW_ARCHIVE_CREATION_NOW: NO
ALLOW_TAG_NOW: NO
ALLOW_POST_TAG_PRUNING_NOW: NO
~~~

## 13. Planning-task validation record

~~~text
Preflight baseline: PASS
branch: dev
HEAD / origin/dev / advertised SHA: c7f940c0c48900a0cfb7eac86aac745c8006629c
ahead / behind before task: 0 / 0
worktree / staged before task: clean / empty
local close tag: absent
remote close tag: absent
archive directory: absent
scope invariants: PASS / 3 OF 3
terminal factsource inventory: PASS / 12 paths / 10 stale at planning baseline
machine dependency references to Stage-QDR-9 current source docs: 0 at planning baseline
full tests: NOT_RERUN
PostgreSQL/Testcontainers: NOT_RERUN
reused technical evidence: 30633947829 / 1243 / 0 / 0 / 0 / PostgreSQL 17.10
quality validate: PASS / 19 OF 19 REACTOR
Checkstyle: PASS / 0 VIOLATIONS
Spotless: PASS
local planning commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT PUBLISHED
push: NOT PERFORMED / NOT AUTHORIZED
tag: NOT CREATED / NOT AUTHORIZED
archive: NOT CREATED / NOT AUTHORIZED
~~~

## 14. Readiness decision

~~~text
STAGE_QDR_9_B5_FINAL_CLOSE_PLAN:
DONE

B5_TYPE:
FINAL_CLOSE_BATCH

B5_TECHNICAL_IMPLEMENTATION_REQUIRED:
NO

B5_GOVERNANCE_CLOSE_EXECUTION_REQUIRED:
YES

STAGE_QDR_9_FUNCTIONAL_CLOSE_ELIGIBLE:
YES

FORMAL_CAPACITY_REQUIRED_FOR_STAGE_CLOSE:
NO

TERMINAL_FACTSOURCE_SYNC_PLAN:
PASS

ARCHIVE_PACKET_PLAN:
PASS

TAG_CLOSE_SEQUENCE:
PASS

POST_TAG_CLEANUP_PLAN:
PASS

ALLOW_B5_GOVERNANCE_CLOSE_EXECUTION:
YES / NEXT TASK ONLY

ALLOW_STAGE_QDR_9_CLOSE_NOW:
NO

ALLOW_ARCHIVE_CREATION_NOW:
NO

ALLOW_TAG_NOW:
NO

ALLOW_POST_TAG_PRUNING_NOW:
NO

ALLOW_NEXT_STAGE_PLAN:
NO

ALLOW_V16_IMPLEMENTATION:
NO

ALLOW_RETENTION_IMPLEMENTATION:
NO

ALLOW_CAPACITY_EXECUTION:
NO

ALLOW_SERVER_DEPLOYMENT:
NO

PRODUCTION_CAPACITY:
NOT_PROVEN

Next concrete action:
DH-STAGE-QDR-9-B5-STAGE-FINAL-CLOSE
~~~
