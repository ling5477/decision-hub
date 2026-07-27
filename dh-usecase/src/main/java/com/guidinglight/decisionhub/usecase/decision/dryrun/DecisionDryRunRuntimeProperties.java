package com.guidinglight.decisionhub.usecase.decision.dryrun;

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
    Set<String> allowedSourceEnvironmentPairs,
    Set<String> allowedTenantEnvironmentPairs,
    Set<String> allowedTenantSourceEnvironmentTriples,
    int memoryCapBytes) {

  private static final String CANONICAL_SOURCE = "NQ_DRYRUN";
  private static final String PAIR_SEPARATOR = "::";

  /**
   * 创建 dry-run runtime 配置快照并执行 fail-closed source 合同校验。
   *
   * <p>Why：HMAC 直接对 wire-level source 签名，runtime policy 绝不能再对该身份字段做大小写归一化；否则
   * 配置侧与认证侧会对同一请求给出不同结论。配置条目的外层空白可以移除，但其余字符必须保持不变并精确等于
   * {@value #CANONICAL_SOURCE}。任何非 canonical source、空白条目、非法 pair 或 pair/allowlist 矛盾都必须在
   * Spring bean 创建阶段抛出异常，不能静默改写或降级为 runtime fallback。
   */
  public DecisionDryRunRuntimeProperties {
    allowedSources = normalizeConfiguredSources(allowedSources);
    allowedTenantSourcePairs = normalizeConfiguredPairs(allowedTenantSourcePairs);
    allowedSourceEnvironmentPairs = normalizeConfiguredSourceEnvironmentPairs(allowedSourceEnvironmentPairs);
    allowedTenantEnvironmentPairs = normalizeConfiguredTenantEnvironmentPairs(allowedTenantEnvironmentPairs);
    allowedTenantSourceEnvironmentTriples =
        normalizeConfiguredTenantSourceEnvironmentTriples(allowedTenantSourceEnvironmentTriples);
    validatePairsBelongToAllowlist(allowedSources, allowedTenantSourcePairs);
    validateEnvironmentBindings(
        allowedSources,
        allowedTenantSourcePairs,
        allowedSourceEnvironmentPairs,
        allowedTenantEnvironmentPairs,
        allowedTenantSourceEnvironmentTriples);
    memoryCapBytes = memoryCapBytes <= 0 ? 32 * 1024 : memoryCapBytes;
  }

  /** Preserves the former runtime-property contract; absent environment authorization remains denied. */
  public DecisionDryRunRuntimeProperties(
      final boolean enabled,
      final boolean productionEnabled,
      final boolean killSwitchEnabled,
      final boolean devOrTestProfile,
      final Set<String> allowedSources,
      final Set<String> allowedTenantSourcePairs,
      final int memoryCapBytes) {
    this(
        enabled,
        productionEnabled,
        killSwitchEnabled,
        devOrTestProfile,
        allowedSources,
        allowedTenantSourcePairs,
        Set.of(),
        Set.of(),
        Set.of(),
        memoryCapBytes);
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
    return allowedSources.contains(source);
  }

  /**
   * 校验 tenant/source pair 是否在 allowlist 中。
   *
   * @param tenantId request tenant。
   * @param source request source。
   * @return 是否允许。
   */
  public boolean tenantSourceAllowed(final String tenantId, final String source) {
    return allowedTenantSourcePairs.contains(requestPair(tenantId, source));
  }

  /** Returns whether the signed source is authorized for the signed environment. */
  public boolean sourceEnvironmentAllowed(final String source, final String environment) {
    return allowedSourceEnvironmentPairs.contains(source + PAIR_SEPARATOR + environment);
  }

  /** Returns whether the authenticated tenant is authorized for the signed environment. */
  public boolean tenantEnvironmentAllowed(final String tenantId, final String environment) {
    return allowedTenantEnvironmentPairs.contains(requestPair(tenantId, environment));
  }

  /** Returns whether the complete tenant/source/environment binding is explicitly authorized. */
  public boolean tenantSourceEnvironmentAllowed(
      final String tenantId, final String source, final String environment) {
    return allowedTenantSourceEnvironmentTriples.contains(
        requestPair(tenantId, source) + PAIR_SEPARATOR + environment);
  }

  /**
   * 规范配置中的 source allowlist，但不改写 source 的大小写。
   *
   * <p>空集合表示当前 profile 没有任何可用 source，例如 production 默认关闭；这与含有空字符串或空白字符串的
   * 配置条目不同，后者是显式错误，必须拒绝启动。
   */
  private static Set<String> normalizeConfiguredSources(final Set<String> configuredSources) {
    if (configuredSources == null) {
      return Set.of();
    }
    return configuredSources.stream()
        .map(DecisionDryRunRuntimeProperties::canonicalConfiguredSource)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * 规范 tenant/source pair 配置，并保留 source 的 wire case。
   *
   * <p>Pair 只允许 {@code tenant:NQ_DRYRUN} 或 {@code tenant::NQ_DRYRUN}。tenant 外层空白沿用既有配置语义
   * 移除；source 始终委托给 canonical source 校验，不能使用 alias 或大小写回退。
   */
  private static Set<String> normalizeConfiguredPairs(final Set<String> configuredPairs) {
    if (configuredPairs == null) {
      return Set.of();
    }
    return configuredPairs.stream()
        .map(DecisionDryRunRuntimeProperties::normalizePairConfig)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String normalizePairConfig(final String configuredPair) {
    if (configuredPair == null) {
      throw new IllegalArgumentException("dry-run tenant/source pair must not be null");
    }
    final String trimmedPair = configuredPair.trim();
    if (trimmedPair.isEmpty()) {
      throw new IllegalArgumentException("dry-run tenant/source pair must not be blank");
    }
    final String[] parts =
        trimmedPair.contains(PAIR_SEPARATOR)
            ? trimmedPair.split(PAIR_SEPARATOR, -1)
            : trimmedPair.split(":", -1);
    if (parts.length != 2 || parts[0].trim().isEmpty()) {
      throw new IllegalArgumentException("dry-run tenant/source pair must contain tenant and source");
    }
    return parts[0].trim() + PAIR_SEPARATOR + canonicalConfiguredSource(parts[1]);
  }

  private static Set<String> normalizeConfiguredSourceEnvironmentPairs(
      final Set<String> configuredPairs) {
    return normalizeEnvironmentBindings(configuredPairs, 2, true);
  }

  private static Set<String> normalizeConfiguredTenantEnvironmentPairs(
      final Set<String> configuredPairs) {
    return normalizeEnvironmentBindings(configuredPairs, 2, false);
  }

  private static Set<String> normalizeConfiguredTenantSourceEnvironmentTriples(
      final Set<String> configuredTriples) {
    return normalizeEnvironmentBindings(configuredTriples, 3, false);
  }

  private static Set<String> normalizeEnvironmentBindings(
      final Set<String> configuredBindings, final int partCount, final boolean sourceFirst) {
    if (configuredBindings == null) {
      return Set.of();
    }
    return configuredBindings.stream()
        .map(binding -> normalizeEnvironmentBinding(binding, partCount, sourceFirst))
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String normalizeEnvironmentBinding(
      final String configuredBinding, final int partCount, final boolean sourceFirst) {
    if (configuredBinding == null || configuredBinding.isBlank()) {
      throw new IllegalArgumentException("dry-run environment binding must not be blank");
    }
    final String[] parts = configuredBinding.trim().split(PAIR_SEPARATOR, -1);
    if (parts.length != partCount) {
      throw new IllegalArgumentException("dry-run environment binding has an invalid part count");
    }
    final int environmentIndex = partCount - 1;
    final String environment = parts[environmentIndex].trim();
    if (!("DEV".equals(environment) || "TEST".equals(environment))) {
      throw new IllegalArgumentException("dry-run environment must be DEV or TEST");
    }
    final String first = parts[0].trim();
    if (first.isEmpty()) {
      throw new IllegalArgumentException("dry-run environment binding identity must not be blank");
    }
    if (sourceFirst && !CANONICAL_SOURCE.equals(first)) {
      throw new IllegalArgumentException("dry-run source/environment source must be NQ_DRYRUN");
    }
    if (partCount == 3 && !CANONICAL_SOURCE.equals(parts[1].trim())) {
      throw new IllegalArgumentException("dry-run tenant/source/environment source must be NQ_DRYRUN");
    }
    return String.join(PAIR_SEPARATOR, java.util.Arrays.stream(parts).map(String::trim).toList());
  }

  /**
   * 建立 request lookup key，不对不可信 source 做 trim、大小写变换或 alias 映射。
   *
   * <p>只保留既有 tenant 外层空白处理，使 runtime lookup 与认证后的 tenant binding 兼容；source 是否允许完全由
   * exact set membership 决定。
   */
  private static String requestPair(final String tenantId, final String source) {
    final String tenant = tenantId == null ? "" : tenantId.trim();
    return tenant + PAIR_SEPARATOR + source;
  }

  /**
   * 接受一个配置 source 条目并验证它是唯一允许的 canonical value。
   *
   * <p>只允许 trim 配置项的外层空白；任何空值、空白、alias、lowercase、mixed-case 或未知 source 都是启动配置
   * 错误。此方法不得被用于 request wire value。
   */
  private static String canonicalConfiguredSource(final String configuredSource) {
    if (configuredSource == null) {
      throw new IllegalArgumentException("dry-run source must not be null");
    }
    final String trimmedSource = configuredSource.trim();
    if (!CANONICAL_SOURCE.equals(trimmedSource)) {
      throw new IllegalArgumentException(
          "dry-run source must be exactly " + CANONICAL_SOURCE + " after outer whitespace is removed");
    }
    return trimmedSource;
  }

  /**
   * 验证每个显式 tenant/source pair 都由 source allowlist 支持。
   *
   * <p>这避免配置在 endpoint 启动后才以 `SOURCE_DENIED` 暴露自相矛盾状态；空 pair 集合仍允许表达当前 profile
   * 不接受任何 tenant/source 组合。
   */
  private static void validatePairsBelongToAllowlist(
      final Set<String> configuredSources, final Set<String> configuredPairs) {
    for (final String pair : configuredPairs) {
      final String source = pair.substring(pair.indexOf(PAIR_SEPARATOR) + PAIR_SEPARATOR.length());
      if (!configuredSources.contains(source)) {
        throw new IllegalArgumentException("dry-run tenant/source pair source is not in allowedSources");
      }
    }
  }

  private static void validateEnvironmentBindings(
      final Set<String> configuredSources,
      final Set<String> configuredTenantSourcePairs,
      final Set<String> configuredSourceEnvironmentPairs,
      final Set<String> configuredTenantEnvironmentPairs,
      final Set<String> configuredTriples) {
    for (final String sourceEnvironment : configuredSourceEnvironmentPairs) {
      final String source = sourceEnvironment.substring(0, sourceEnvironment.indexOf(PAIR_SEPARATOR));
      if (!configuredSources.contains(source)) {
        throw new IllegalArgumentException("dry-run source/environment source is not in allowedSources");
      }
    }
    for (final String triple : configuredTriples) {
      final String[] parts = triple.split(PAIR_SEPARATOR, -1);
      final String tenantSource = parts[0] + PAIR_SEPARATOR + parts[1];
      final String sourceEnvironment = parts[1] + PAIR_SEPARATOR + parts[2];
      final String tenantEnvironment = parts[0] + PAIR_SEPARATOR + parts[2];
      if (!configuredTenantSourcePairs.contains(tenantSource)
          || !configuredSourceEnvironmentPairs.contains(sourceEnvironment)
          || !configuredTenantEnvironmentPairs.contains(tenantEnvironment)) {
        throw new IllegalArgumentException(
            "dry-run tenant/source/environment triple requires all pair authorizations");
      }
    }
  }
}
