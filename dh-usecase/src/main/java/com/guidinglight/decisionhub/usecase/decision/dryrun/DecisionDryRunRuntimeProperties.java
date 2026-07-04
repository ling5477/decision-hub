package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Integration-1 limited dry-run runtime endpoint 的 feature gate 配置快照。
 *
 * <p>Why：endpoint 必须 default disabled，且只有 dev/test profile 可显式开启。生产 profile 即使误设
 * enabled=true，也必须依赖 productionEnabled 的独立闸门；killSwitch 打开时立即 fail-closed。
 *
 * @param enabled endpoint 是否启用。
 * @param productionEnabled 是否允许非 dev/test profile 启用；默认必须为 false。
 * @param killSwitchEnabled kill switch 是否打开。
 * @param devOrTestProfile 当前是否为 dev/test profile。
 * @param allowedSources source allowlist；只允许 dev/test 下的 NQ_DRYRUN 等 review-gated source。
 * @param allowedTenantSourcePairs tenant/source pair allowlist。
 * @param memoryCapBytes decisionContext 内存上限。
 */
public record DecisionDryRunRuntimeProperties(
    boolean enabled,
    boolean productionEnabled,
    boolean killSwitchEnabled,
    boolean devOrTestProfile,
    Set<String> allowedSources,
    Set<String> allowedTenantSourcePairs,
    int memoryCapBytes) {

  /** 创建配置快照并归一化 allowlist。 */
  public DecisionDryRunRuntimeProperties {
    allowedSources =
        allowedSources == null
            ? Set.of()
            : allowedSources.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(DecisionDryRunRuntimeProperties::normalizeSource)
                .collect(Collectors.toUnmodifiableSet());
    allowedTenantSourcePairs =
        allowedTenantSourcePairs == null
            ? Set.of()
            : allowedTenantSourcePairs.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(DecisionDryRunRuntimeProperties::normalizePairConfig)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    memoryCapBytes = memoryCapBytes <= 0 ? 32 * 1024 : memoryCapBytes;
  }

  /**
   * 判断 endpoint 当前是否允许处理 runtime request。
   *
   * @return true 表示 feature flag/profile/kill switch 均允许；false 表示必须 POLICY_DENIED。
   */
  public boolean runtimeEnabled() {
    if (!enabled || killSwitchEnabled) {
      return false;
    }
    return devOrTestProfile || productionEnabled;
  }

  /**
   * 校验 source 是否在名称 allowlist 中。
   *
   * @param source request source。
   * @return 是否允许。
   */
  public boolean sourceAllowed(final String source) {
    return allowedSources.contains(normalizeSource(source));
  }

  /**
   * 校验 tenant/source pair 是否在 allowlist 中。
   *
   * @param tenantId request tenant。
   * @param source request source。
   * @return 是否允许。
   */
  public boolean tenantSourceAllowed(final String tenantId, final String source) {
    return allowedTenantSourcePairs.contains(normalizePair(tenantId, source));
  }

  private static String normalizePairConfig(final String configuredPair) {
    final String[] parts =
        configuredPair.contains("::")
            ? configuredPair.split("::", 2)
            : configuredPair.split(":", 2);
    if (parts.length != 2) {
      return "";
    }
    return normalizePair(parts[0], parts[1]);
  }

  private static String normalizePair(final String tenantId, final String source) {
    final String tenant = tenantId == null ? "" : tenantId.trim();
    return tenant + "::" + normalizeSource(source);
  }

  private static String normalizeSource(final String source) {
    return source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
  }
}
