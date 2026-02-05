# dh-domain 补丁（为 Run 增加 decisionRecord）

你遇到的编译错误：
- 无法解析 Run.getDecisionRecord / setDecisionRecord

原因：之前的 FULL_BUNDLE 没有包含 dh-domain 模块的改动。

## 方式 1：git apply（推荐）
在仓库根目录执行：
    git apply patches/dh-domain_Run.java.patch

前提：你的 Run.java 路径为：
    dh-domain/src/main/java/com/guidinglight/decisionhub/domain/run/Run.java

若你的模块名/路径不同，把 patch 文件里的路径改成真实路径即可。

## 方式 2：直接覆盖文件
用本包的：
    dh-domain/src/main/java/com/guidinglight/decisionhub/domain/run/Run.java
覆盖你仓库中的同路径文件。

## 后续（正式持久化）
为了重启/读库不丢 decisionRecord：建议在 run 表增加列：
- decision_record_json (JSON/LONGTEXT)
并在 rehydrate 时一并装载（后续我也能给你完整持久化补丁）。
