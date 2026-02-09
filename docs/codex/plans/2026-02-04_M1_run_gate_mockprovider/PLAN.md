# PLAN: 2026-02-04_M1_run_gate_mockprovider
- 目标：Run 创建闭环 + Golden verify 绑定 + Checkstyle 闸门
- 验证：scripts\verify.ps1

## 2026-02-08 配置规则修复计划（最小侵入）
1. 修复 `docs/logging-spec-final.md` 编码，恢复可读中文内容。
2. 移除 `application*.yml` 中数据库明文默认密码，仅保留环境变量占位。
3. 统一 Checkstyle 规则入口，避免 `config/checkstyle` 与 `dh-bom/checkstyle` 漂移。
4. 更新 `STATUS.json` 与 `CHANGE_NOTES.md`，执行 `scripts/verify.ps1` 并回填 `lastVerify`。
