package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Limited dry-run runtime 的启动期 profile 合同。
 *
 * <p>只允许恰好一个 {@code dev} 或 {@code test} profile。空 profile、未知 profile、production profile、多个
 * profile 以及 production gate 误开均拒绝，避免混合 profile 意外放行。
 *
 * @param activeProfiles Spring active profiles 快照。
 * @param productionEnabled production gate；B3 必须保持 false。
 */
public record RuntimeEnvironmentPolicy(Set<String> activeProfiles, boolean productionEnabled) {

  /** 规范 profile 快照并拒绝 null 元素。 */
  public RuntimeEnvironmentPolicy {
    activeProfiles =
        activeProfiles == null
            ? Set.of()
            : activeProfiles.stream()
                .map(Objects::requireNonNull)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * 判断是否为唯一受允许的 dev/test 环境。
   *
   * @return true 仅表示 limited runtime environment gate 允许。
   */
  public boolean allowsLimitedRuntime() {
    if (productionEnabled || activeProfiles.size() != 1) {
      return false;
    }
    final String profile = activeProfiles.iterator().next();
    return "dev".equals(profile) || "test".equals(profile);
  }
}
