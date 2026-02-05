# APPLY_GUIDE
1) 解压到仓库根目录（允许覆盖 AGENTS.md）
2) 手动合并 root pom.xml：
   - 增加 module: dh-eval
   - exec-maven-plugin 绑定 verify 执行 GoldenCaseRunnerMain
   - checkstyle 插件使用 dh-bom/checkstyle/checkstyle.xml（或 config/checkstyle/checkstyle.xml）
3) 运行 scripts\verify.ps1

## 5) Git/PR 规范化（新增）
- docs/dev/GIT_CONVENTION.md
- docs/dev/BRANCH_CONVENTION.md
- .github/PULL_REQUEST_TEMPLATE.md
- tools/git/commit-template.txt（可选）
- docs/dev/GIT_SETUP.md
