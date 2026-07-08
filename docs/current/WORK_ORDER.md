# Decision Hub 当前工单

## 1. 唯一下一步

```text
current task: DH-STAGE-QDR-4-B1-REPLAY-EVALUATION-DOMAIN-CONTRACTS / DONE
next action: DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN
mode: IMPLEMENTATION + DOMAIN_CONTRACTS_ONLY + NO_MIGRATION + NO_API
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B1_ONLY / DONE
stage-qdr-4 B2: NOT_STARTED / PLAN_ONLY_NEXT
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
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B1_ONLY / DONE
stage-qdr-4 B2: NOT_STARTED / PLAN_ONLY_NEXT
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

## 4. 下一步允许范围

```text
DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN
只允许 planning / review / freeze 范围
评估 migration / persistence / repository 边界
如需 migration，必须单独 review/freeze
不得直接 B2 implementation
不得跳到 B3 mock gateway regression integration
不得跳到 B4 report/read model
保持 no real HTTP / provider / SDK / Agent / LangGraph / LIVE
```

## 5. Stage-qdr-4 后续禁止范围

```text
禁止未经明确授权继续修改 Java 生产代码
禁止未经明确授权继续修改 Java 测试代码
禁止在 B2 plan 前新增 migration
禁止修改 V1-V8 migration
禁止未经 review/freeze 新增 V9 migration
禁止新增 API / Controller / REST endpoint
禁止新增真实 HTTP outbound
禁止新增真实 provider client
禁止新增 Provider SDK
禁止启动 LangGraph / AutoGen / CrewAI
禁止启动 Agent runtime
禁止修改 NQ
禁止直接进入 B2 implementation
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
