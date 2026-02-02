# Codex 执行协议（decision-hub）

目标：让 Codex 依据本仓库 contracts/docs/golden_cases 实现功能，避免上下文漂移。

## 规则
1) 先读 docs/dev_guide.md + contracts/README.md + golden_cases/README.md
2) 任何新增 API：先更新 contracts/openapi.yaml，再实现 Controller
3) 任何新增输出结构：先更新 contracts/schema，再实现序列化对象
4) 任何破坏性修改：必须新增/更新 golden_cases
5) 变更必须通过：mvn -DskipTests clean package

## 输出要求
- 输出改动文件清单
- 说明是否触及：contracts / schema / golden_cases
