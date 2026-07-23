# Stage-QDR-8 Post-Tag Source Recovery

## 状态

- Stage-QDR-8 原 stage tag：`dh-stage-qdr-8-close`（保持不变）
- tag target：`7b6066d1062fe49d16d1f093c8b3170354854376`（保持不变）
- recovery type：`POST_TAG_ARCHIVE_SOURCE_COMPLETION`
- recovered source documents：`3 / 3`
- source/recovered Git blob mismatch：`0`
- SHA-256 mismatch：`0`

## 恢复原因与边界

Post-tag pruning 删除了 `docs/current` 中已关闭阶段的 process documents，但原 archive packet 未逐文件保存全部原始执行依据。本次恢复完全从阶段 annotated tag 提取原始 Git blob，并将其放入工作树中可搜索的 `source-documents/` archive packet。

- 不恢复文件到 `docs/current`。
- 不修改文件正文、标题、状态、换行或历史表述。
- 不修改历史阶段结论，不重新打开任何 batch。
- 不修改、移动、删除或重新创建原 stage tag。
- 不形成新的阶段 tag。
- annotated tag 保留不可变恢复能力；本目录补齐 searchable archive source packet。

逐文件来源、blob 与 SHA-256 见 `SOURCE_MANIFEST.md` 和 `SOURCE_SHA256SUMS`。
