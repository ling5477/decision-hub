# DH Stage-QDR-9 B4 Upstream Module Dependency Scope Erratum

## 1. 任务结论

~~~text
task: DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-MAVEN-DEPENDENCY-SCOPE-RETRY
classification: DOCUMENTATION / SCOPE_CONTRACT_ERRATUM / MAVEN_DEPENDENCY_DIRECTION_REVIEW / SECURITY_DOMAIN_DEPENDENCY_FREEZE / STRONG_ENVIRONMENT_TYPE_AUTHORIZATION / CURRENT_FACTSOURCE_ALIGNMENT
starting HEAD: 1971f3dc29fb690dcbd64cb7b0c62c797d81cbb1
parent: 75c2449972c4b6f15689144478f31c0b7edf8126
origin/dev: 7624bccba9b865d4b687057f41b96799cb9ba8e3 / FRESHLY VERIFIED
technical changes: 0
POM changes: 0
test changes: 0
migration changes: 0
V16: NOT CREATED
upstream contract: NOT IMPLEMENTED
~~~

本 erratum 只冻结 future implementation 的 Maven 模块依赖范围。首次 implementation retry 在任何代码写入或提交前停止：`dh-security` 无法解析 canonical `FeedbackEnvironment`，其根因是 `dh-security/pom.xml` 不在原 `46 / 46` 冻结 `WRITE_ALLOWLIST`。本轮没有添加依赖，也没有修改任何 POM。

## 2. Maven reactor 与依赖方向审计

已实际执行：

```powershell
mvn -B -ntp -pl dh-security -am dependency:tree
mvn -B -ntp -pl dh-domain -am dependency:tree
```

两个命令均成功。`dh-security` 的 reactor 为 `dh-bom -> dh-common -> dh-security`；`dh-domain` 的 reactor 为 `dh-bom -> dh-common -> dh-domain`。根 reactor 的固定模块顺序也将 `dh-domain` 放在 `dh-security` 之前。

| 审计项 | 实际结果 |
| --- | --- |
| `dh-domain` production direct dependency | `com.guidinglight:dh-common:1.0.0-SNAPSHOT` |
| `dh-domain` production transitive dependency | Jackson annotations / databind；无 `dh-security`、Spring、JDBC、Web 或 security implementation |
| `dh-security` production direct dependency | `com.guidinglight:dh-common:1.0.0-SNAPSHOT` |
| `dh-security` production transitive dependency | Jackson annotations / databind；无 Spring、JDBC、Web 或 provider runtime |
| `dh-domain` 直接或传递依赖 `dh-security` | NO |
| 加入 `dh-security -> dh-domain` 是否形成 Maven cycle | NO |
| `dh-domain` 是否只暴露本任务所需的 foundational contract | YES |
| 新依赖是否引入 Spring/JDBC/Web runtime concern | NO |
| root `dependencyManagement` 是否可省略 reactor version | NO；现有 reactor direct dependency 均显式使用 `${project.version}` |

结论：依赖方向审计通过。未来 `dh-security -> dh-domain` 是单向、compile 范围的依赖，不改变 `dh-domain` 的独立性或 current layering。

## 3. 冻结的唯一 future POM 变更

### B4_UPSTREAM_MODULE_DEPENDENCY_SCOPE

~~~text
dh-security/pom.xml
~~~

唯一允许的 future semantic change 为：在 `dh-security/pom.xml` 增加现有 reactor dependency `com.guidinglight:dh-domain:${project.version}`，使用默认 `compile` scope，使 security 编译期可引用 canonical `FeedbackEnvironment`。实现时必须沿用当前 `dh-security` 与其他 reactor module 的显式 `${project.version}` 约定；不以 attachment 的示例坐标或省略 version 的形式覆盖当前仓库事实。

```xml
<dependency>
  <groupId>com.guidinglight</groupId>
  <artifactId>dh-domain</artifactId>
  <version>${project.version}</version>
</dependency>
```

禁止增加 root POM、`dh-domain/pom.xml`、`dependencyManagement`、repository、plugin、profile、Java version、Surefire/Failsafe、shade/assembly 或任何 optional/provided workaround。若 implementation 仍需要另一个 POM 文件，必须停止并报告 `STAGE_QDR_9_B4_UPSTREAM_MODULE_DEPENDENCY_SCOPE_BLOCKER`。

## 4. strong environment type import boundary

`dh-security` 从 `dh-domain` 的 future upstream contract 仅可使用：

~~~text
com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment
~~~

不得新增 `FeedbackExecutionScope`、decision aggregate、attribution record、persistence port、retention、replay/evaluation 或 trading contract 的 domain import。`FeedbackExecutionScope` 继续只能在 verified root boundary 创建，authenticator 内不得创建、推断或保存未验证字符串。禁止复制 security-local enum、回退为 `String`、使用 `Object`/`Map` 或默认 `DEV`。

## 5. Scope 历史与最终不变量

~~~text
Reference-liveness design: 27 / 27 PASS
Producer environment contract: 31 / 31 PASS
Producer source design: 36 / 36 PASS
Trusted upstream authority: 42 / 42 PASS
Corrected upstream implementation scope: 46 / 46 PASS / BLOCKED BY UNAUTHORIZED MAVEN DEPENDENCY FILE / SUPERSEDED FOR IMPLEMENTATION ACCEPTANCE
B4_UPSTREAM_MODULE_DEPENDENCY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
dh-security/pom.xml ∈ WRITE_ALLOWLIST: PASS
EFFECTIVE_UPSTREAM_SCOPE_INVARIANTS: 47 / 47 PASS
TASK_SCOPE_DESIGN_INVALID: NO
~~~

本结论不等同于 implementation acceptance：HMAC environment binding、`FeedbackExecutionScope`、AUDIT propagation、registry、retention、V16、API、scheduler 与 automatic learning 全部仍未实现或未授权。

## 6. 后续限制

下一任务仅为 `DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-IMPLEMENTATION-RETRY-2`。它可在精确 47-file scope 内实施并验证上游合同；不授权 V16、B4 milestone review retry、B4 publication、B5、API、scheduler、automatic learning、real HTTP/provider/NQ、Agent/LangGraph、Paper 或 LIVE。
