# Decision Hub 当前工单

## 1. 唯一下一步

```text
current task: DH-STAGE-QDR-4-PLAN / DONE / PLAN_ACCEPTED
next action: DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
mode: WORK_ORDER_ONLY / DOCUMENTATION_ONLY
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 implementation: NOT_STARTED / NO
current workspace: F:/project/decision-hub
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
stage-qdr-4 implementation: NOT_STARTED / NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

## 3. Stage-qdr-4 Plan 结果

```text
recommended direction: QDR Replay / Evaluation / Regression Baseline
stage-qdr-4 name: QDR Replay / Evaluation / Regression Baseline
deferred direction B: Model gateway observability / provider readiness hardening
deferred direction C: real provider dry-run readiness plan
deferred direction D: Agent / LangGraph preparation
```

## 4. 下一步 work order 允许范围

```text
读取当前事实源
把 stage-qdr-4 plan 转化为 implementation work order
明确 B1-B5 文件边界、review 触发规则、验证命令和回滚方式
保持 no real HTTP / provider / SDK / Agent / LangGraph / LIVE
保持 no code diff / no migration diff，除非用户另行明确授权 implementation work order 之外的变更
输出 work-order-only result
```

## 5. Stage-qdr-4 禁止范围

```text
禁止新增或修改 Java 生产代码
禁止新增或修改 Java 测试代码
禁止新增 migration
禁止修改 V1-V8 migration
禁止新增 V9 migration
禁止新增 API / Controller / REST endpoint
禁止新增真实 HTTP outbound
禁止新增真实 provider client
禁止新增 Provider SDK
禁止启动 LangGraph / AutoGen / CrewAI
禁止启动 Agent runtime
禁止修改 NQ
禁止启动 stage-qdr-4 implementation
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
.\mvnw.cmd -v
```

`mvnw.cmd` 当前仍为 `UNUSABLE / P2 TOOLING RISK`；验证使用系统 Maven `mvn`。不得使用 `-DskipTests` 或 `-DskipITs` 后写成完整通过。
