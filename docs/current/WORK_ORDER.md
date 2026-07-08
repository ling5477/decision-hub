# Decision Hub 当前工单

## 1. 唯一下一步

```text
next action: DH-STAGE-QDR-3-B5-CLOSE-REVIEW
mode: REVIEW_ONLY / RETRY
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
current workspace: F:/Project/decision-hub
```

## 2. 当前前置状态

```text
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B5: READY FOR RETRY
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

## 3. B5 Close Review 允许范围

```text
读取当前事实源
复核 B1-B4 evidence
复核 docs/current 状态一致性
复核 no real HTTP / provider / SDK / Agent / LangGraph / LIVE
复核 no code diff / no migration diff
复核 quality validate
输出 close review decision
```

## 4. B5 Close Review 禁止范围

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
禁止把 stage-qdr-3 写成 ACCEPTED
禁止把 stage-qdr-3 final close 写成 CLOSED
禁止启动 stage-qdr-4
禁止启用 LIVE
禁止 git push
禁止 git commit，除非用户另行明确授权
```

## 5. 可阻断事实源

B5 close review 只应以 `FACTSOURCE_POLICY.md` 中的 `CURRENT_FACTSOURCE_CAN_BLOCK_CLOSE` 集合作为默认 blocker source。`WORKLOG.md`、`ROADMAP.md`、`API.md`、`DB_SCHEMA.md` 默认 supporting only。`docs/gates/**` 为 historical records，不作为默认 blocker；`docs/archive/**` 不再作为 QDR 当前归档标准。

## 6. 验收命令

```powershell
git status --short
git diff --check
git diff --stat
git diff --name-only
git diff --cached --name-only
stale facts scan required by DH-DOCS-GOVERNANCE-ARCHIVE-STAGE-QDR-3-PRE-CLOSE
mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

`mvnw.cmd` 当前仍为 `UNUSABLE / P2 TOOLING RISK`；验证使用系统 Maven `mvn`。不得使用 `-DskipTests` 或 `-DskipITs` 后写成完整通过。
