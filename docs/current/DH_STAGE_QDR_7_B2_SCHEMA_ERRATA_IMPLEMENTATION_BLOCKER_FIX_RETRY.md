# DH Stage-QDR-7 B2 Schema Errata Implementation Blocker Fix Retry

> task: `DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY`
> mode: `DOC_FIX / CURRENT_FACTSOURCE_ALIGNMENT / REPOSITORY_ENTRY_NORMALIZATION / REVIEW_RETRY_PREPARATION`
> baseline: `d827fce03e6a48ab5d1277cc61b18cce4a7798a1`
> implementation result: `DONE / REVIEW_RETRY_2_PENDING`

## 1. 目标与边界

本任务只清除仓库入口和current factsources中未标为historical/consumed的旧阶段状态。schema errata callback、migration和PostgreSQL技术证据均不需要修改。

```text
Stage-QDR-7 B1: FROZEN
B2 schema errata implementation: DONE
B2 schema errata review: TECHNICAL_PASS / FACTSOURCE_FIX_PENDING
B2 milestone acceptance: NOT_YET
capacity acceptance: NOT_ALLOWED
B3: NOT_ALLOWED
schema errata technical implementation: PASS
schema errata independent acceptance: BLOCKED only by factsource drift
callback/migration code changes: NOT_REQUIRED
current task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY
next task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY-2
```

`FACTSOURCE_FIX_PENDING`表示对齐结果仍需独立review retry-2确认，不表示本轮文档仍存在已知current conflict，也不表示B2已`ACCEPTED`。

## 2. Authority hierarchy

```text
Primary current-state authority:
1. docs/current/STATUS.md
2. docs/current/WORK_ORDER.md

Entry / execution guidance:
- README.md
- docs/current/README.md
- docs/current/CODEX_PROJECT_INSTRUCTIONS.md
- CLAUDE.md
- AGENTS.md

Historical / supporting:
- dated records below each current authority block
- completed plan / work order / review snapshots
- docs/current/ROADMAP.md
- docs/current/TESTING.md
- docs/current/WORKLOG.md
- docs/current/ARCHIVE_INDEX.md
- docs/current/API.md
- docs/current/DB_SCHEMA.md
- docs/gates/**
```

入口和执行指导必须与主权威一致，但不得覆盖`STATUS.md`和`WORK_ORDER.md`。历史字段即使保留`current task`或`next action`原文，也只能在明确historical/consumed区域内解释。

## 3. 修复结果

- root/current README：顶部唯一current authority已对齐；Stage-QDR-2至Stage-QDR-6累计状态改为明确历史摘要；完成的Stage-QDR-7 plan/work order标记为historical/consumed。
- CODEX：顶部authority、Stage-QDR-7 current task与next task已对齐；旧Stage-QDR-4入口移动为明确`Historical / Consumed`区域。
- FACTSOURCE_POLICY：明确`STATUS.md`/`WORK_ORDER.md`主权威，入口与执行指导不能覆盖；旧cleanup/current state替换为Stage-QDR-7 B2链路。
- CLAUDE：移除Stage3-B3/Integration-0-PLAN当前入口，声明自身仅为execution guidance，并同步Integration-0 `CLOSED / ACCEPTED`及GateN rebase边界。
- AGENTS：仅最小同步schema errata review和next task；保留全部工程、安全与skill纪律。
- STATUS/WORK_ORDER/ROADMAP/TESTING/WORKLOG：新增本任务current记录；所有旧日期/旧current标题统一置于明确historical区域。
- 历史review：`DH_STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW_RETRY.md`及更早`BLOCKED`结论未修改。

## 4. Stale wording classification

附件指定扫描表达式的每条命中必须归入以下唯一类别：

```text
CURRENT_CONFLICT       当前authority或入口仍把旧阶段/旧任务表述为current
HISTORICAL_MARKED      位于明确historical/consumed区域或被policy定义为历史快照
NEGATIVE_SAFETY_STATEMENT  明确禁止未启动runtime、旧入口复活或越级授权
```

最终验收要求`CURRENT_CONFLICT = 0`。历史Stage-QDR-4归档路径、previous review中的原始blocker说明、已消费work order以及`Integration-1 NOT STARTED`等负向安全陈述可以保留，但不得成为当前入口。

## 5. Validation

```text
git diff --check: PASS
stale wording classification: PASS / 378_MATCHES_CLASSIFIED
HISTORICAL_MARKED: 323
NEGATIVE_SAFETY_STATEMENT: 55
CURRENT_CONFLICT: 0
mvn -ntp -Pquality validate: PASS / 19_OF_19 / 17.669s
Checkstyle: PASS / 0_VIOLATIONS
Spotless: PASS
Maven tests: NOT_RUN / NOT_REQUIRED
PostgreSQL/Testcontainers: NOT_RUN / NOT_REQUIRED
```

## 6. Boundary confirmation

```text
callback change: NO
Java production change: NO
Java test change: NO
V1-V14 change: NO
new migration: NO
API/Controller/DTO/OpenAPI change: NO
HMAC/nonce/source contract change: NO
capacity benchmark: NOT_RUN
Stage-QDR-7 B3: NOT_ENTERED
external HTTP/provider/NQ/Agent/LangGraph/LIVE: NOT_TOUCHED
push: NO
tag: NO
```

## 7. Readiness

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_BLOCKER_FIX_RETRY: DONE
ROOT_README_ALIGNMENT: PASS
CURRENT_README_ALIGNMENT: PASS
CODEX_INSTRUCTIONS_ALIGNMENT: PASS
FACTSOURCE_POLICY_ALIGNMENT: PASS
CLAUDE_ALIGNMENT: PASS
AGENTS_ALIGNMENT: PASS
CURRENT_FACTSOURCE_CONSISTENCY: PASS
HISTORICAL_REVIEW_PRESERVATION: PASS
SECURITY_BOUNDARY: PASS
ALLOW_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW_RETRY_2: YES
ALLOW_MILESTONE_REVIEW_RETRY_3_NOW: NO
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

验证通过后的唯一下一任务：

```text
DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY-2
```
