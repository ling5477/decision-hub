# DH Post-Stage-QDR-11 下一阶段规划

## 1. 任务分类与授权

```text
Task: DH-POST-STAGE-QDR-11-NEXT-STAGE-PLANNING
Task classification: DOCUMENTATION / PLANNING_ONLY
POST_MILESTONE_TRANSITION_REVIEW: YES
CURRENT_AUTHORITY_RECONSTRUCTION: YES
CODE_REALITY_GAP_REVIEW: YES
CANDIDATE_WORKSTREAM_COMPARISON: YES
SINGLE_NEXT_STAGE_SELECTION: YES
PRODUCTION_READINESS_CLASSIFICATION: YES
SECURITY_BOUNDARY_FREEZE: YES
DOCS_ONLY_CHANGE: YES
IMPLEMENTATION: NO
NQ_CHANGE / REAL_HTTP / REAL_PROVIDER: NO / NO / NO
FEEDBACK_LEARNING / AGENT / LANGGRAPH / PAPER / LIVE: NO / NO / NO / NO / NO
```

本规划只选择并冻结下一阶段，不执行 capacity、不修改技术实现，也不把任何历史 deferred 能力自动恢复为当前能力。

## 2. Plugins selected

```text
recommended by workflow router: GitHub + Documents + Notion
actually used plugins: NONE / LOCAL-FIRST EVIDENCE SUFFICIENT
GitHub result: gh CLI READ_ONLY / CLEANUP EXACT-SHA CI 31314059545 PASS
Codex Security result: NOT_EXECUTED / PLANNING_ONLY / REUSED STAGE-QDR-11 SEALED PASS AS HISTORICAL EVIDENCE
CodeRabbit result: NOT_EXECUTED / DOCS-ONLY PLANNING
Skills: nq-dh-workflow-router + dh-docs-writer
```

未调用外部文档或 Notion connector；GitHub 只用于核验远端 refs 与既有 CI，没有远端写操作。

## 3. Current fact verification

| Fact | Verified result |
|---|---|
| repository | 当前工作区仓库 / `decision-hub` |
| branch | `dev` |
| HEAD | `6f0097c4a5b0eed354505d59c8a53a9f0f593557` |
| origin/dev | `6f0097c4a5b0eed354505d59c8a53a9f0f593557` |
| advertised SHA | `6f0097c4a5b0eed354505d59c8a53a9f0f593557` |
| ahead / behind | `0 / 0` |
| worktree / staged | `clean / empty` |
| Stage-QDR-10 | `CLOSED / ACCEPTED / ARCHIVED / TAGGED` |
| Stage-QDR-11 | `CLOSED / ACCEPTED / ARCHIVED / TAGGED` |
| Stage-QDR-11 implementation | `9b50fc7f51ebad3257aa77d1c2874d71385c81e9` |
| Stage-QDR-11 archive close | `d02133a358cd5a5c1f0db25c471367cd40880d70` |
| Stage-QDR-11 cleanup | `6f0097c4a5b0eed354505d59c8a53a9f0f593557` |
| close tag | `dh-stage-qdr-11-close` / annotated / local + remote verified |
| peeled tag target | `d02133a358cd5a5c1f0db25c471367cd40880d70` |
| archive | `docs/gates/stage-qdr-11/` / present / self-contained packet |
| current process residue | `0` / 已删除两个 Stage-QDR-11 process sources |
| cleanup exact-SHA CI | `31314059545` / PASS / test job `93246323299` + quality job `93246323302` |
| active P0/P1 | `0 / 0` |
| formal capacity | `NOT_EXECUTED` |
| production capacity / ready | `NOT_PROVEN / NO` |
| mismatches | `0` |

基线门通过，没有触发 `POST_STAGE_QDR_11_BASELINE_BLOCKED`。

## 4. Scope model

### 4.1 Read scope

- 当前 authority 入口与 allowlisted planning factsources。
- 生产代码、测试、配置、migration、POM、CI、capacity harness 仅只读。
- `docs/gates/stage-qdr-11/` 只用于 archive/tag 验真，不作为新授权来源。
- 不扫描 `target`、`.git`、generated output、secret-bearing paths 或 NQ 仓库。

### 4.2 Write allowlist

```text
docs/current/DH_POST_STAGE_QDR_11_NEXT_STAGE_PLAN.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

`FACTSOURCE_POLICY.md` 与 `ARCHIVE_INDEX.md` 不修改：本轮没有 authority policy 或 archive index 规则变化。

### 4.3 Scope invariants

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
```

本轮 current scan 只对 8 个写入 terminal block 的 planning factsources 加新 authority，并以本计划为详细合同；`AGENTS.md`、`CLAUDE.md`、`FACTSOURCE_POLICY.md`、`ARCHIVE_INDEX.md` 作为只读基线/政策/归档入口，不越过用户冻结的 write allowlist。

## 5. Code reality inspected

成熟度词汇：`R0 DOC_ONLY`、`R1 SKELETON`、`R2 INTERNAL_IMPLEMENTATION`、`R3 WIRED_INTERNAL`、`R4 PERSISTED_AND_TESTED`、`R5 RUNTIME_REACHABLE`、`R6 PRODUCTION_HARDENED`。

| Area | Code evidence | Maturity | Current conclusion |
|---|---|---:|---|
| decision pipeline | `DecisionOrchestrator`、`DecisionDryRunService`、`DecisionDryRunController`，受保护 endpoint 为 `/api/ai/decision-dry-runs` | R5（dev/test limited runtime） | 只读 deterministic mock 路径可达；prod 默认关闭，不是 production-ready |
| audit / trace / snapshot | JDBC audit/replay query、read model、canonical snapshot assembly 与 V5/V10/V11 | R4 | tenant/correlation/persistence 已测试，未发现新的 linkage 断链 |
| QDR replay / evaluation / regression | replay contract、deterministic replay、regression evaluation/read model、V9 persistence | R4 | internal persisted/tested；无授权 runtime consumer |
| provider trust / readiness / observability | trust policy、readiness guard、observability report 进入 evidence aggregate | R3-R4 | mock-only internal；real provider、真实账单/运行监控均未接入 |
| consolidated evidence | `DecisionFeedbackEvidenceService` -> `DecisionFeedbackEvidenceAggregate` | R4 | single authority；bounded policy 与完整 correlation fail-closed |
| internal acceptance | `DecisionFeedbackInternalAcceptanceService` -> `DecisionEvidenceReplayInternalReport` | R3 | Spring bean 已装配、read-only；production caller 为 0 |
| internal acceptance consumer | facade 本身是唯一 injectable internal entry point | R3 | 当前无 downstream business consumer；在 acceptance authorization = NONE 下不是需新增抽象的断链 |
| limited runtime safety | feature flag、production gate、kill switch、HMAC、nonce、persistent rate/idempotency、no-side-effect | R5（dev/test） | DH inbound 已存在；NQ cross-repo runtime 未开始 |
| memory / deadline / queue / backpressure | memory cap 32768 bytes、deadline 5000 ms、max concurrency 4、bounded queue 8、max wait 100 ms | R5（limited runtime） | hard ceilings 已实现；尚无 current exact-SHA formal capacity verdict |
| persistent multi-instance rate limiting | JDBC fixed-window admission 与 exact identity | R5 | endpoint 已调用，store unavailable/commit unknown fail-closed |
| formal capacity harness | `qdr7-capacity-acceptance` profile、PowerShell finalizer、15 mandatory scenario registry、frozen thresholds | R4 | implemented/tested；formal acceptance 仍未执行成功 |
| reference-liveness | feedback write-time JDBC reference validation | R4（窄边界） | 没有 global liveness registry；也没有 dangling/ref failure 现实证据 |
| retention / cleanup | nonce 惰性清理、guard bounded cleanup primitives、retention configuration | R3-R4 | 无 scheduler、无 production row growth/storage pressure 证据 |
| structured feedback | outcome、attribution、historical bounded evidence 已持久化并测试 | R4 | inbound learning side effects 已隔离；promotion/online learning 不授权 |
| NQ-DH integration | DH signed inbound dry-run endpoint + fake/disabled NQ clients | DH R5 / cross-repo R0 | NQ worktree/PR 未在本仓库授权范围内验证；必须另立 integration workstream |
| Agent prerequisites | legacy `AgentTask`/`AgentArtifact`/in-memory Stage2 services 存在 | R1-R2 | `AgentTaskSpec`、definition/capability、tool auth、context budget、sandbox/egress、inheritance 等前置合同缺失 |
| CI / Testcontainers / tooling | default test + quality CI；PostgreSQL/Testcontainers exact-SHA CI 通过 | R5（普通 CI） | formal capacity profile 未进入普通 CI；Maven wrapper 仍是已知 P2 tooling debt |
| scheduler / operational alerts | `dh-scheduler` 仅 `Job` skeleton；无 guard cleanup scheduler | R1 | 当前未形成单一高价值、已授权的 operational stage |

### 5.1 六个特别问题

1. **Stage-QDR-11 internal acceptance 是否有合理 internal caller/use？**
   - 有合理的 injectable internal facade/use contract，并已作为单 bean 装配；但下游 production caller 为 `0`。因此成熟度是 `R3 WIRED_INTERNAL`，不是业务 runtime 已消费。
2. **decision/evidence/acceptance 是否仍有真实断链？**
   - 未发现 duplicate authority、legacy fallback、correlation 缺失或 audit/replay linkage 断裂。当前没有 acceptance authorization 或 runtime consumer 需求，production caller 为 0 不构成当前断链。
3. **是否已达到继续增加 acceptance 抽象价值很低的状态？**
   - 是。未来真实流程需要 acceptance 时，应直接消费既有 facade；在需求出现前不得新增 parallel aggregate/report/facade。
4. **当前阻止 production readiness 的最主要缺口？**
   - 当前 exact SHA 缺少正式、可复验的 capacity/resource-safety verdict。普通回归和 qualification 不能替代 formal acceptance。
5. **当前阻止 limited runtime 下一步的缺口？**
   - DH 本地 limited runtime 已有入口和安全 guard，但 prod 保持关闭；任何 NQ-DH 下一步还缺当前 cross-repo authority、NQ client/worktree/PR 事实和独立 integration stage，且 formal capacity 未关闭。
6. **当前阻止 Agent Runtime Contract 阶段的缺口？**
   - Java/control-plane 前置合同大面积缺失：immutable task spec、definition/capability、artifact ref、tool authorization、context budget、checkpoint/pause-resume、failure taxonomy、sandbox/egress、child-agent inheritance 均未形成可审查基线。

## 6. Candidate assessment

| Candidate | Code evidence | Maturity | Real gap | Dependency | API/DB impact | Necessity | Classification | Rank |
|---|---|---:|---|---|---|---|---|---:|
| A. Production Capacity / Resource Safety | 15-scenario harness、frozen thresholds、persistent guards、bounded admission、historical qualification 15/15 + 94/94 | runtime R5 / formal gate R4 | current exact-SHA formal verdict 缺失 | qualified local environment、cached `postgres:17`、clean exact SHA | none for execution | 当前最高价值、可正式验收 | `PRODUCTION_READINESS_GATE` | 1 |
| B. NQ-DH Limited Runtime Readiness | DH inbound endpoint、feature/kill/no-side-effect/idempotency/rate/resource guards；fake/disabled NQ clients | DH R5 / cross-repo R0 | NQ current authority、client/worktree/PR 未验证 | NQ repo + 独立 integration stage | future cross-repo/API contract | 真实但不可混入 DH-only stage | `CROSS_REPO_INTEGRATION_WORKSTREAM` | 2 |
| G. Platform / Operational Hardening | normal CI、Testcontainers、quality；wrapper P2、scheduler skeleton、无 runtime alerting | R1-R5 mixed | 多个分散 debt，无单一当前阻断目标 | 需另做目标收敛 | possible tooling only | 不优先于 formal gate | `DEFERRED_INDEPENDENT_CAPABILITY` | 3 |
| C. Agent Runtime Contract Baseline | legacy AgentTask/Artifact/Stage2 services；核心 prerequisite types 缺失 | R1-R2 | control-plane contracts 缺失 | formal prerequisites planning + security review | future contract/API unknown | 未来前置，但当前过早 | `AGENT_PHASE_PREREQUISITE` | 4 |
| D. Structured Feedback Evolution | outcome/attribution/historical evidence R4；learning mutation 已隔离 | R4 read-only | candidate artifact/promotion flow 未实现，但无当前业务需求 | explicit offline approval demand | likely DB/contracts later | 当前不必要 | `DEFERRED_INDEPENDENT_CAPABILITY` | 5 |
| E. Reference-Liveness | feedback write-time reference validation R4 | R4 narrow / global R0 | 无 global registry，但无 dangling/ref failure | actual failure evidence | likely migration/repository | 无现实触发 | `DEFERRED_INDEPENDENT_CAPABILITY` | 6 |
| F. Retention / Lifecycle | bounded cleanup primitives、retention config、nonce lazy cleanup | R3-R4 | 无 scheduler/全局 lifecycle，但无 row/storage pressure | growth/compliance evidence | likely DB/scheduler | 无现实触发 | `DEFERRED_INDEPENDENT_CAPABILITY` | 7 |
| H. Decision / Evidence / Acceptance Follow-up | single aggregate + wired internal facade；legacy direct path retired | R3-R4 | downstream production caller 0，但当前不授权 consumer | future explicit use case | none now | 继续抽象价值低 | `NOT_NEEDED` | 8 |

## 7. Selected next stage

```text
SELECTED_WORKSTREAM: PRODUCTION_CAPACITY_RESOURCE_SAFETY
NEXT_STAGE_NAME: DH-STAGE-QDR-12-FORMAL-CAPACITY-RESOURCE-SAFETY-ACCEPTANCE
NEXT_STAGE_TYPE: PRODUCTION_READINESS_GATE
CORE_OBJECTIVE: 对当前 exact SHA 使用既有冻结 harness 完成一次可复验、fail-closed 的 formal capacity/resource-safety acceptance；不启用 production runtime。
REAL_GAPS_CLOSED: CURRENT_EXACT_SHA_FORMAL_CAPACITY_VERDICT + RESOURCE_THRESHOLD_EVIDENCE + RECOVERY/CONTENTTION/BOUNDED_ADMISSION_ACCEPTANCE
PREREQUISITES: CLEAN EXACT SHA + EXACT-SHA CI PASS + QUALIFIED WINDOWS/DOCKER/JAVA/MAVEN ENVIRONMENT + CACHED postgres:17 + FROZEN CRITERIA HASH
CONFIDENCE: HIGH
```

### 7.1 Why selected / why now

- 当前功能链已覆盖 decision、audit/trace/snapshot、replay/regression、consolidated evidence 与 internal acceptance；继续围绕 acceptance 建模会过度设计。
- limited runtime 的 persistent guards 与 resource ceilings 已实现，capacity harness 也不是 skeleton：mandatory registry 为 15，threshold contract 和 artifact/finalizer 均存在。
- 历史 qualification 已完整通过，但 formal 终局仍为 `capacityAcceptanceExecuted=false`；这正是可独立关闭、不会污染 NQ/Agent/feedback 的真实 readiness gap。
- 该阶段只证明冻结 profile 内的正式容量，不声称 production-ready，不接真实 provider/NQ，也不改变外部契约。

### 7.2 Rejected and deferred alternatives

- H `NOT_NEEDED`：不创建 Stage-QDR-12 evidence/acceptance abstraction。
- B 真实但必须是独立 cross-repo integration workstream；当前缺 NQ authority，不能混入 DH capacity stage。
- C 为未来 Agent phase prerequisite；不得因长期目标提前引入 Agent/LangGraph。
- D/E/F/G 保持独立 deferred；没有业务压力、failure evidence 或单一明确目标，不从历史计划恢复。

## 8. Frozen batch plan

### B1 — Exact-SHA scope and work-order freeze

- 核验 branch、HEAD/origin、clean/staged、exact-SHA CI、frozen criteria hash、15-scenario registry 和 formal command。
- 冻结 read/validation/write/fixable blocker/current scan scopes；production/test/config/harness 默认全为只读。
- 产出唯一 implementation work order；本 batch 不执行 formal capacity。

### B2 — Qualified environment and harness admission

- 执行环境 preflight、contract tests、implementation-validation/qualification admission，验证 Docker、cached `postgres:17`、Java 21、Maven 3.9.x、PowerShell、CPU/memory 与无残留资源。
- qualification 只证明 harness 可进入，不产生 formal verdict。
- preflight 或 qualification 失败即 fail-closed，形成精确 blocker；不得改阈值、跳 scenario 或现场扩 scope。

### B3 — Formal 15-scenario capacity acceptance

- 在 clean exact SHA 上只执行一次冻结 formal command。
- 必须执行 15/15 mandatory scenarios、94/94 threshold comparisons、correctness zero-tolerance、resource sampling、full regression、quality、manifest、secret scan 和 teardown。
- 任何 missing/partial/failure/blocker 都不得写成 PASS；不得用 ordinary CI 或 qualification 替代。

### B4 — Evidence adjudication, security boundary, and final close

- 校验 artifact schema、SHA-256 manifest、criteria hash、secret scan、resource residual、exact-SHA binding 和 15-scenario ledger。
- 复核没有 external HTTP、real provider、NQ、learning、Agent/LangGraph、Paper/LIVE 或生产开关变化。
- PASS 时只关闭 profile-bounded formal capacity gate；FAIL/BLOCKED 时保留证据并另立精确 blocker，不在 final close 现场修复。

```text
B5: NOT_NEEDED
NEXT_STAGE_BATCH_COUNT: 4
```

## 9. Write and security boundaries

```text
PRODUCTION_WRITE_BOUNDARY: NONE
TEST_WRITE_BOUNDARY: NONE DURING FORMAL EXECUTION
HARNESS/CONFIG/THRESHOLD_WRITE_BOUNDARY: NONE / FROZEN READ_ONLY
GENERATED_EVIDENCE: target/qdr7-capacity-acceptance/<run-id>/ / UNTRACKED
MIGRATION_IMPACT: NONE
API_IMPACT: NONE
REPOSITORY_IMPACT: NONE
NQ_IMPACT: NONE
PROVIDER_IMPACT: NONE / DETERMINISTIC MOCK ONLY
AGENT_IMPACT: NONE
```

Security boundaries：

- localhost-only、cached image、单一 local PostgreSQL/Testcontainers；禁止外部数据库。
- 禁止 real HTTP/provider、NQ client/mutation/DB、Agent/LangGraph、Paper/LIVE。
- production-enabled 保持 `false`，kill switch 默认 deny；formal acceptance 不授权开启 endpoint。
- 不读取或打印 credential；harness artifact 必须完成 secret scan。
- frozen thresholds、criteria hash、scenario registry 不得在执行中调整。
- 失败、partial、missing artifact、environment mismatch、manifest mismatch、secret finding 必须非 0 / fail-closed。

## 10. Non-goals

- 不实现新 evidence/acceptance aggregate、report、facade 或 consumer。
- 不启用 NQ-DH runtime，不改 NQ，不发真实 HTTP。
- 不接 real provider，不把 mock readiness 提升为 provider readiness。
- 不恢复 feedback learning 或任何 Experience/Pheromone/FailureCase 自动 mutation/promotion。
- 不实现 reference-liveness registry、retention scheduler 或 global cleanup。
- 不创建 Agent/LangGraph runtime contract，不启用 Paper/LIVE。
- 不把 formal profile PASS 写成 production deployment、production SLA、production SLO 或 production-ready。

## 11. Test matrix

| Gate | Required evidence | Pass condition |
|---|---|---|
| baseline | git/local/remote/tag/CI | clean exact SHA，ahead/behind 0/0 |
| criteria | frozen criteria document + JSON hash | exact frozen hash，无 diff |
| registry | scenario registry + dispatcher contract | mandatory `15 / 15`，固定顺序 |
| environment | preflight | `26 / 26` 或 current frozen total 全通过；cached `postgres:17` |
| admission | harness contract / implementation validation / qualification | lifecycle、dispatcher、artifact、teardown 全通过，但仍 `capacityAcceptanceExecuted=false` |
| formal rate | concurrency 1/2/4/8/16，15 measured rounds | 所有 throughput/latency thresholds 通过 |
| correctness | quota、isolation、source、nonce、idempotency、cleanup | oversell/cross-scope/partial/deadlock/unexpected error 均 0 |
| recovery | Hikari/PostgreSQL/contention/restart/post-recovery | frozen thresholds 与 round counts 全通过 |
| resource | JVM/Maven/Surefire/Docker/free memory | frozen caps/headroom 全通过，无 OOM |
| regression | formal scenario 内 full reactor test | 19/19，0 failures/errors/skipped，Testcontainers 实跑 |
| quality | formal scenario 内 quality | 19/19，Checkstyle 0，Spotless PASS |
| artifacts | schema/ledger/manifest/secret/teardown | 0 missing/mismatch/findings/residual |

## 12. Review triggers

仅以下条件触发 standalone review：

```text
migration/schema
API/Controller
security boundary
cross-repo runtime
P0/P1
stage final close
```

本阶段预期只触发 security boundary 与 stage final close。若出现 migration/API/cross-repo/P0/P1，立即阻断并另立任务。

## 13. Rollback

- formal run 不修改 production/test/harness/config；失败时删除或保留 `target` evidence 由 work order 明确，均不进入 Git commit。
- 文档回滚使用本阶段独立 planning/work-order/final-close commit 的普通 revert；不得重写历史。
- capacity verdict 不改 runtime 开关，因此无 production runtime rollback。
- 若 environment 或 harness blocker 出现，保持 `FORMAL_CAPACITY=NOT_PROVEN` 并转入精确 blocker task。

## 14. Final close condition

```text
FORMAL_COMMAND: EXECUTED ONCE ON CLEAN EXACT SHA
MANDATORY_SCENARIOS: 15 OF 15 PASS
THRESHOLD_COMPARISONS: ALL EXECUTED / ALL PASS
CORRECTNESS: PASS / ZERO-TOLERANCE INVARIANTS 0
RESOURCE_SAFETY: PASS WITHIN FROZEN PROFILE
FULL_REGRESSION: 19 OF 19 / 0 FAILURES / 0 ERRORS / 0 SKIPPED
QUALITY: 19 OF 19 / CHECKSTYLE 0 / SPOTLESS PASS
ARTIFACTS / MANIFEST / SECRET / TEARDOWN: PASS / 0 MISSING / 0 MISMATCH / 0 FINDINGS / 0 RESIDUAL
ACTIVE_P0_P1: 0 / 0
PRODUCTION_READY: NO
```

Stage PASS 只允许写：`FORMAL_CAPACITY=PASS_WITHIN_FROZEN_PROFILE`。在没有 production deployment、真实 provider、NQ integration 与独立 operational gate 前，`PRODUCTION_READY` 继续为 `NO`。

## 15. Archive/tag/current-cleanup discipline

1. B1-B4 正常小步提交，普通 batch 不打 tag。
2. stage final close PASS 后形成 self-contained archive packet。
3. archive close commit 先于 annotated tag；tag 只能在独立 tag-close task 授权后创建。
4. tag target 必须包含完整 archive packet。
5. tag 后清理 `docs/current` 的 stage process sources，验证 residue 0，才能启动再下一阶段 planning。

## 16. Readiness decision

```text
POST_STAGE_QDR_11_NEXT_STAGE_PLAN: DONE
SELECTED_NEXT_WORKSTREAM: PRODUCTION_CAPACITY_RESOURCE_SAFETY
SELECTED_NEXT_STAGE: DH-STAGE-QDR-12-FORMAL-CAPACITY-RESOURCE-SAFETY-ACCEPTANCE
NEXT_STAGE_SCOPE: FROZEN
NEXT_STAGE_BATCH_COUNT: 4
ALLOW_NEXT_STAGE_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_NEXT_STAGE_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_EXECUTION_NOW: NO
ALLOW_REFERENCE_LIVENESS_NOW: NO
ALLOW_RETENTION_NOW: NO
ALLOW_FEEDBACK_LEARNING_NOW: NO
ALLOW_NQ_RUNTIME_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

## 17. Risks

| Risk | Control |
|---|---|
| incorrect next-stage selection | code-first matrix；capacity selected because harness+guards exist but formal verdict does not |
| evidence/acceptance overengineering | H classified `NOT_NEEDED`；禁止新增抽象 |
| capacity overpromotion | PASS 仅限 frozen profile；production-ready 保持 NO |
| cross-repo scope pollution | NQ direction classified independent workstream；NQ impact NONE |
| feedback-learning reactivation | all learning mutations remain unauthorized |
| reference-liveness overdesign | no dangling evidence -> deferred |
| premature Agent/LangGraph | prerequisite gaps recorded；phase remains NO |
| documentation overgrowth | only 1 plan + minimal terminal authority blocks |
| authority drift | 8 planning terminal blocks + 1 detailed plan，同一 frozen decision |

## 18. Next concrete action

```text
DH-STAGE-QDR-12-FORMAL-CAPACITY-RESOURCE-SAFETY-ACCEPTANCE-IMPLEMENTATION-WORK-ORDER
```

建议 commit message：

```text
docs(qdr): plan post-stage-qdr-11 next stage
```

## 19. Files inspected and changed

主要只读证据：

- current authority：`AGENTS.md`、`CLAUDE.md`、root/current `README.md`、`STATUS.md`、`WORK_ORDER.md`、`ROADMAP.md`、`TESTING.md`、`WORKLOG.md`、`CODEX_PROJECT_INSTRUCTIONS.md`、`FACTSOURCE_POLICY.md`、`ARCHIVE_INDEX.md`。
- Stage-QDR-11 archive：`docs/gates/stage-qdr-11/` 文件清单、local/remote tag 与 cleanup commit/CI。
- decision/evidence/acceptance：`DecisionPipelineWiringConfig.java`、`DecisionFeedbackInternalAcceptanceService.java`、evidence/report/replay/read-model/JDBC adapters 及相关架构/集成测试。
- limited runtime/resource safety：`DecisionDryRunController.java`、`DecisionDryRunRuntimeWiringConfig.java`、`PersistentDecisionDryRunRateLimiter.java`、runtime policy/queue/deadline classes 与 application profiles。
- capacity：scenario/threshold/artifact JSON、acceptance criteria、PowerShell harness、`Qdr7CapacityAcceptanceIT.java`、root/`dh-app` POM profile 与 CI workflow。
- reference/retention/feedback/Agent：feedback reference/attribution/history adapters、guard cleanup、scheduler skeleton、AgentTask/Artifact 与 Agent wiring。

实际修改：本计划与 8 个 planning terminal-block factsources；未修改 policy、archive index 或任何技术文件。

## 20. Validation

```text
git status before write: CLEAN / STAGED EMPTY
git diff --check: PASS
allowlisted docs: 9
unexpected files: 0
technical diff: 0
archive diff: 0
close tag: UNCHANGED / ANNOTATED / LOCAL+REMOTE VERIFIED / PEELED d02133a358cd5a5c1f0db25c471367cd40880d70
planning terminal blocks: 8 OF 8 / IDENTICAL SHA-256 / 0 CONFLICTS
detailed plan: 1 / PRESENT
scope invariants: PASS / 3 OF 3
quality command: mvn -B -ntp -Pquality validate
quality: PASS / 19 OF 19 REACTOR SUCCESS
Checkstyle / Spotless: 0 / PASS
full tests: NOT_RERUN / PLANNING_ONLY
reused implementation CI: 31312337732 / 1338 / 0 FAILURES / 0 ERRORS / 0 SKIPPED / PostgreSQL 17.10
reused close CI: 31313584409 / PASS
reused cleanup CI: 31314059545 / PASS / EXACT SHA 6f0097c4a5b0eed354505d59c8a53a9f0f593557
local commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT_PUSHED
push / tag: NO / NO
```

Boundary confirmation：Stage-QDR-10/11 保持 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`；archives/tags 不变；没有修改生产代码、测试、API、migration、Repository、contracts、POM 或 workflow；没有新增 evidence/acceptance 抽象，没有恢复 learning，没有执行 capacity/reference-liveness/retention，没有修改 NQ，也没有接入 HTTP、Provider、Agent、LangGraph、Paper/LIVE。
