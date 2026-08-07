# Rollback

Before tag creation, any close-document defect is corrected with an ordinary new commit or `git revert`; history rewrite and force push are forbidden.

After `dh-platform-hardening-feedback-ingest-atomicity-close` is published, the tag is immutable and must never be moved, overwritten, deleted, or recreated. A defect is handled by a new commit and independent review. The post-tag cleanup commit may be reverted normally, which restores current process documents without changing the close tag or archived copies.

No production database, external service, or runtime rollback applies because this close contains documentation governance only.
