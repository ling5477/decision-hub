# VERIFY（验收口径）

## 适用范围
- `taskType=delivery`：必须执行 `scripts/verify.ps1`。
- `taskType=analysis-only`：允许跳过 verify，但必须在 `STATUS.json:lastVerify` 记录 `result=SKIPPED` 与 `summary`。

## 执行命令
- Windows：`scripts\verify.ps1`
- 本质执行：`mvn verify`
- 必须覆盖：编译+测试、Checkstyle、Golden 回归

## 结果回填（STATUS.json）
- 成功：`result=PASS`
- 失败：`result=FAIL`，并填写 `failedAtStep`、`errorCode`、`nextAction`
- 跳过：`result=SKIPPED`，并说明原因
