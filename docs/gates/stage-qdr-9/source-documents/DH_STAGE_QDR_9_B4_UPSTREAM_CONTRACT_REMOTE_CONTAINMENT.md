# DH Stage-QDR-9 B4 Upstream Contract Remote Containment

## 1. 决策

~~~text
task:
DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-REMOTE-CONTAINMENT-DECISION

selected option:
OPTION 1A / ORDINARY REVERT OF IMPLEMENTATION ONLY

published implementation:
549ed5a3224ce3ce375452dcf57629c73e3101d0

published exact-SHA CI:
30283326199 / PASS

CI interpretation:
REGRESSION PASS ONLY
DOES NOT CLOSE SECURITY REVIEW

implementation revert:
df921f275c61d67cebbb95c0924391866a6d09dc

history rewrite:
NONE

design/scope chain:
PRESERVED

current B4:
IMPLEMENTATION REVERTED / REVIEW BLOCKED
~~~

本任务选择普通 revert，只撤销已发布实现 `549ed5a…`，不撤销此前六条设计与 scope 提交。该选择保留完整 Git 审计轨迹，同时把当前技术树恢复到 B3 safe baseline。未执行 `reset`、`rebase`、`amend`、`force push`、merge、stash 或 tag。

## 2. 被保留的设计与 scope 链

以下提交继续作为已发布历史和后续 scope-design 证据：

~~~text
2cc75dcc8c2228af43e79d7163d7489bf67ebc97
9e7dfdc557b4a8d5c0768bdd3414f775971c0d73
75c2449972c4b6f15689144478f31c0b7edf8126
1971f3dc29fb690dcbd64cb7b0c62c797d81cbb1
675430a8a8e6cceaab75bb72c1fc1bf64af2da46
94ca8f0dc84b58b60eef7727440cc4d276dd8c37
~~~

这些提交记录了 Option B、安全设计、scope 从早期集合发展到 `48 / 48 FROZEN` 的过程及 blocker 轨迹。保留它们不等于当前树已实现 signed environment contract。

## 3. 发布与审查事实

`549ed5a…` 曾经发布，且 GitHub Actions run `30283326199` 对该 exact SHA 为 PASS。该结果只证明已配置构建、回归和质量任务通过，不证明安全里程碑审查通过。

~~~text
publication authorization:
NOT PROVABLE

pre-publication milestone review:
NOT COMPLETED

retrospective review:
BLOCKED

governance P1:
PUBLISHED BEFORE MILESTONE REVIEW
~~~

不得把 containment 写成“实现从未发布”，也不得把绿灯 CI 写成 P1/P2 已关闭。

## 4. 技术 findings

### Technical P1 — OPEN

一个 P1 finding group 包含两个受影响的安全语义：

1. persistent rate / idempotency identity 使用 deployment guard environment，而不是 verified signed request environment；
2. QDR7 rate-audit compatibility path 丢失 verified environment，持久化 audit evidence 无法区分 `DEV` 与 `TEST`。

### Technical P2 — OPEN

Dry-run replay key 未包含 environment。两个分别合法、分别签名的 `DEV` 与 `TEST` 请求在 tenant/source/path/nonce/requestId 相同的条件下会折叠到同一 replay namespace。

本任务只记录和 containment，不实现 P1/P2 修复。安全报告的严重度用于漏洞报告；Stage 治理的 P1/P2 标签继续保持原有权威，二者不得相互覆盖。

## 5. Revert 结果

普通 revert 完整撤销 `549ed5a…` 引入的 production Java、Java tests、`dh-security/pom.xml` dependency、wiring 和 terminal-factsource 中间状态。设计与 scope 文档不在 revert 范围。

相对 containment technical baseline `7624bccba9b865d4b687057f41b96799cb9ba8e3`：

~~~text
production Java diff:
0

Java test diff:
0

POM diff:
0

config diff:
0

workflow diff:
0

current upstream implementation code:
NOT PRESENT

dh-security -> dh-domain dependency:
NOT PRESENT

FeedbackExecutionScope implementation:
NOT PRESENT

V16 / registry / retention:
NOT CREATED / NOT IMPLEMENTED / NOT PRESENT
~~~

V1–V15、B1、B2 与 B3 的当前技术状态不变。

## 6. 本地验证

在 revert commit 上执行并通过：

~~~text
mvn -B -ntp -pl dh-domain -am test
mvn -B -ntp -pl dh-security -am test
mvn -B -ntp -pl dh-usecase -am test
mvn -B -ntp -pl dh-infra,dh-app -am test
mvn -B -ntp test
mvn -B -ntp -Pquality validate
~~~

结果：

~~~text
module regressions:
4 OF 4 PASS

full regression:
19 OF 19 REACTOR SUCCESS
1228 TESTS
0 FAILURES
0 ERRORS
0 SKIPPED

PostgreSQL/Testcontainers:
REAL EXECUTION / POSTGRESQL 17.10 / 0 MANDATORY SKIPS

ArchitectureTest:
PASS

StageQdr9FeedbackArchitectureTest:
PASS

quality:
19 OF 19 REACTOR SUCCESS
CHECKSTYLE 0
SPOTLESS PASS
~~~

`1228` 来自本轮 fresh full regression 修改的 Surefire XML；旧 `target` 输出中的 stale reports 未计入。

## 7. 当前权威与边界

~~~text
Stage-QDR-9 B1 / B2 / B3:
CLOSED / ACCEPTED / PUBLISHED

Stage-QDR-9 B4:
IMPLEMENTATION REVERTED / REVIEW BLOCKED

trusted upstream environment design:
FROZEN / PUBLISHED

upstream scope:
48 / 48 FROZEN

FeedbackExecutionScope:
DESIGNED / NOT IMPLEMENTED

signed environment contract:
DESIGNED / NOT IMPLEMENTED

persistent identity environment isolation:
NOT IMPLEMENTED

AUDIT rate-event environment propagation:
NOT IMPLEMENTED

replay namespace environment isolation:
NOT IMPLEMENTED

B4 milestone review retry / publication / B5:
NOT ALLOWED / NOT ALLOWED / NOT ALLOWED

API / scheduler / automatic learning:
NO NEW ENDPOINT / NOT IMPLEMENTED / NOT ALLOWED

QDR-7 B2 capacity:
DEFERRED / KNOWN LIMITATION

production capacity:
NOT PROVEN
~~~

本任务未新增 endpoint、scheduler、automatic learning、migration、V16、reference-liveness registry 或 retention；未接入真实 HTTP、Provider、NQ runtime、Agent、LangGraph、Paper 或 LIVE；未创建 tag。

## 8. 发布纪律

Authority commit 只允许包含本任务批准的文档。发布前必须重新 fetch 并证明：

~~~text
origin/dev:
549ed5a3224ce3ce375452dcf57629c73e3101d0

advertised SHA:
549ed5a3224ce3ce375452dcf57629c73e3101d0

ahead / behind:
2 / 0
~~~

仅允许执行普通：

~~~text
git push origin dev
~~~

发布后必须等待最终 containment authority SHA 的 test 与 quality exact-SHA CI 全绿。该 CI 完成前不得启动后续 scope design。

## 9. 风险与回滚

- `549ed5a…` 仍保留在历史中，这是 ordinary revert 的预期审计属性；当前分支树不再包含其实现。
- P1/P2 仍为 OPEN。任何重试必须先独立冻结 identity、audit 与 replay namespace 的完整 scope。
- 设计链与当前实现状态必须分开解释，禁止把 `FROZEN / PUBLISHED` 设计写成 `IMPLEMENTED`。
- 回滚 authority 文档可使用新的普通 revert；不得改写历史。
- 不得回滚 `df921f…` 重新暴露 blocked implementation，除非未来独立任务完成 scope、实现、回归、安全审查和显式发布授权。

## 10. 下一步

只有 containment 两提交发布且最终 authority exact-SHA CI 通过后，才允许：

~~~text
DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-IDENTITY-AND-REPLAY-NAMESPACE-SCOPE-DESIGN
~~~

该下一任务仍是 scope-design only，不授权 P1/P2 implementation、V16、B4 milestone review retry、B4 publication 或 B5。
