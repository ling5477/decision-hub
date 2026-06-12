package com.guidinglight.decisionhub.security.nq;

import java.util.Collection;
import java.util.Locale;

/**
 * NQ feedback 防重放 guard 装配类型与选择策略。
 *
 * <p>Why：{@link InMemoryNonceReplayGuard} 当前无界且仅单实例，只能用于 dev/test；生产/多实例必须用持久化实现 （{@code
 * JdbcNonceReplayGuard}）。本枚举把"选哪种实现"的安全策略从装配代码中独立出来，便于直接单元测试， 并保证 fail-closed：配置缺失或无法判定 profile
 * 时绝不退回不安全的 in-memory。
 *
 * <p>本类不持有任何凭证、不发起 IO，只做纯策略判定。
 */
public enum NonceReplayGuardType {
  /** PostgreSQL-backed 持久化实现；非 dev/test 默认值。 */
  JDBC,
  /** 进程内内存实现；仅允许 dev/test。 */
  IN_MEMORY;

  private static final String VALUE_JDBC = "jdbc";
  private static final String VALUE_IN_MEMORY = "in-memory";

  /**
   * 依据配置项与 profile 解析应装配的 guard 类型，fail-closed。
   *
   * <p>规则：
   *
   * <ol>
   *   <li>显式配置 {@code jdbc} -> JDBC（任何 profile 都允许）。
   *   <li>显式配置 {@code in-memory} -> 仅 dev/test 允许，否则抛 {@link IllegalStateException}（启动失败 =
   *       fail-closed）。
   *   <li>未配置（null/blank）-> dev/test 默认 IN_MEMORY；其它（prod / 无 active profile / 无法判定）默认 JDBC。
   *   <li>无法识别的取值 -> 抛 {@link IllegalStateException}，不静默回退。
   * </ol>
   *
   * @param configured 配置项 {@code decisionhub.security.nq-feedback.replay.guard-type}，可为 null/blank。
   * @param devOrTest 当前是否处于 dev/test profile（由 {@link #isDevOrTest} 判定）。
   * @return 解析出的 guard 类型。
   * @throws IllegalStateException 取值非法，或在非 dev/test 下显式要求 in-memory。
   */
  public static NonceReplayGuardType select(final String configured, final boolean devOrTest) {
    if (configured == null || configured.isBlank()) {
      // 配置缺失：非 dev/test 一律走持久化，绝不默认进入无界内存实现。
      return devOrTest ? IN_MEMORY : JDBC;
    }
    final String normalized = configured.trim().toLowerCase(Locale.ROOT);
    switch (normalized) {
      case VALUE_JDBC:
        return JDBC;
      case VALUE_IN_MEMORY:
        if (!devOrTest) {
          throw new IllegalStateException(
              "nonce replay guard-type=in-memory is only permitted under dev/test profiles; "
                  + "non-dev/prod must use jdbc (fail-closed, refusing unbounded in-memory)");
        }
        return IN_MEMORY;
      default:
        throw new IllegalStateException(
            "unknown nonce replay guard-type; expected 'jdbc' or 'in-memory'");
    }
  }

  /**
   * 判定 active profiles 是否包含 dev 或 test。
   *
   * <p>无 active profile 时返回 false（无法判定 -> 视为非 dev/test -> 走安全默认），与 {@link #select} 的 fail-closed
   * 一致。
   *
   * @param activeProfiles Spring active profiles；可为 null。
   * @return 含 dev 或 test 返回 true，否则 false。
   */
  public static boolean isDevOrTest(final Collection<String> activeProfiles) {
    if (activeProfiles == null || activeProfiles.isEmpty()) {
      return false;
    }
    return activeProfiles.stream()
        .filter(p -> p != null)
        .map(p -> p.trim().toLowerCase(Locale.ROOT))
        .anyMatch(p -> p.equals("dev") || p.equals("test"));
  }
}
