# DH Stage-QDR-7 B1 Source Normalization Fix Review

> task: `DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-FIX-REVIEW`
> mode: `REVIEW_ONLY + SECURITY_FIX_REVIEW + SOURCE_CONTRACT_VERIFICATION + CONFIG_FAIL_CLOSED_REVIEW + REGRESSION_VALIDATION`
> review target: `044afba fix(qdr): preserve canonical dry-run source semantics`
> verdict: `PASS / SOURCE_PRODUCTION_DRIFT_CLOSED`

## 1. Review scope and result

本评审只读取并验证`044afba`。提交范围仅包含`DecisionDryRunRuntimeProperties`、其直接`DecisionDryRunRuntimeWiringConfig` CSV解析、四个直接回归测试和Stage-QDR-7 current docs；Controller/DTO、OpenAPI、migration、Repository/JDBC、HMAC production implementation和其他runtime能力均无diff。

```text
SOURCE_NORMALIZATION_FIX_REVIEW: PASS
SOURCE_PRODUCTION_DRIFT: CLOSED
CANONICAL_SOURCE_CONTRACT: PASS
CONFIG_FAIL_CLOSED: PASS
WIRE_EXACT_MATCH: PASS
HMAC_COMPATIBILITY: PASS
NONCE_REPLAY_COMPATIBILITY: PASS
TENANT_SOURCE_ISOLATION: PASS
TEST_EVIDENCE: PASS
SECURITY_BOUNDARY: PASS
STAGE_QDR_7_B1_RUNTIME_CONTRACT: FROZEN
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
```

## 2. Frozen source contract

```text
canonical value: NQ_DRYRUN
wire comparison: case-sensitive exact
wire trim: none
wire aliases: none
configuration outer trim: allowed
configuration case conversion: forbidden
request denial or mismatch: SOURCE_DENIED / 403
invalid application configuration: startup/bean creation failure
```

`DecisionDryRunRuntimeProperties`只对配置条目执行outer trim，随后以`CANONICAL_SOURCE.equals(...)`验证。request lookup使用exact set membership；HMAC在验签前保留body source和header source的原始wire值，并在验签后要求两者逐字符相等。`nq_dryrun`、mixed-case、`NQ-DRYRUN`、未知、空白、full-width或其他Unicode confusable source均不可能等于ASCII `NQ_DRYRUN`；代码没有Unicode normalization、case folding或alias mapping。

完全空的allowlist仍可表示production的deny-all安全状态；非空CSV中的空项、空白项或trailing comma因`split(",", -1)`被保留，并在properties创建时被拒绝。tenant/source pair必须含唯一canonical source，且每个pair必须受source allowlist覆盖。相同canonical条目可被集合去重，不形成可放行的冲突配置；任何包含非canonical值的混合/冲突条目均启动失败。

## 3. Signature, replay, error and logging boundary

`HmacNqDryRunAuthenticator.signatureMaterial`的字段、顺序、UTF-8编码和raw-body SHA-256均无diff。body source仍进入签名材料；source/body修改后旧签名返回`SIGNATURE_INVALID`。签名成功后，header/body source mismatch、空白或未allowlist source返回`SOURCE_DENIED / 403`。replay key和`markIfAbsent`顺序无diff，existing nonce replay回归仍通过；tenant/source pair继续exact isolation。

本提交未新增logger或日志调用。source配置异常只包含固定的canonical定位文案，不回显secret、signature、nonce或raw body；请求路径也未新增任何敏感材料记录。

## 4. Test and runtime evidence

- `DecisionDryRunRuntimePropertiesTest`覆盖canonical配置、outer trim、lowercase/mixed/alias/unknown/blank拒绝、request不trim/不case-normalize，以及pair/allowlist矛盾。
- `DecisionDryRunRuntimeWiringConfigTest`经过实际`DecisionDryRunRuntimeWiringConfig` bean factory覆盖CSV binding、空白、trailing comma与非canonical pair的bean-creation failure。
- `HmacNqDryRunAuthenticatorTest`覆盖canonical wire成功、nonce replay、lowercase/alias/whitespace/header-body mismatch拒绝和source/body篡改签名失效。
- `DecisionDryRunControllerWebMvcTest`经过MockMvc验证canonical request与`SOURCE_DENIED / 403`错误映射。
- `mvn -ntp -pl dh-usecase,dh-security,dh-api,dh-app -am test`通过：15个reactor module、108 tests、0 failures/errors/skips；PostgreSQL 17 Testcontainers和Flyway V1–V11实际执行。
- `mvn -ntp test`通过：19个reactor module；Surefire汇总150个报告、1026 tests、0 failures/errors/skips。
- `mvn -ntp -Pquality validate`通过：19/19 reactor、Checkstyle 0 violations、Spotless check通过。

Unicode/full-width confusable拒绝由ASCII exact equality和不存在Unicode normalization得到静态闭环证明；未来可补一条显式full-width regression作为非阻断性防回归增强，不改变本次`PASS`判定。

## 5. Remaining boundaries and next gate

本评审不解除persistent guards、actual-wiring 2xx harness或post-B2 capacity acceptance的阻断。B2 schema/security review可以开始，但B2 implementation、API/OpenAPI、migration、Repository/JDBC、capacity benchmark retry、real HTTP/provider/NQ/Agent/LangGraph/LIVE继续禁止。

```text
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW
```
