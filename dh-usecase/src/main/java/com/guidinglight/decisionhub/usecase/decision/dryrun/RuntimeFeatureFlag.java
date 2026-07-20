package com.guidinglight.decisionhub.usecase.decision.dryrun;

/**
 * Limited dry-run runtime feature flag 的启动期快照。
 *
 * @param enabled 是否显式启用；默认必须为 false。
 */
public record RuntimeFeatureFlag(boolean enabled) {}
