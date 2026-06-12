package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1：防重放 guard 选择策略测试。
 *
 * <p>覆盖点（对应 plan §4.7 in_memory_guard_only_dev_test）：
 *
 * <ul>
 *   <li>dev/test 允许 in-memory；
 *   <li>prod / 非 dev-test / 无 active profile 不得装配 in-memory；
 *   <li>配置缺失不得 fallback 到不安全实现（非 dev/test 默认 jdbc）。
 * </ul>
 */
class NonceReplayGuardTypeTest {

  @Test
  void explicit_jdbc_is_allowed_under_any_profile() {
    assertEquals(NonceReplayGuardType.JDBC, NonceReplayGuardType.select("jdbc", false));
    assertEquals(NonceReplayGuardType.JDBC, NonceReplayGuardType.select("jdbc", true));
    // 大小写/空白宽容
    assertEquals(NonceReplayGuardType.JDBC, NonceReplayGuardType.select("  JDBC ", false));
  }

  @Test
  void in_memory_guard_only_dev_test() {
    // dev/test 允许 in-memory
    assertEquals(NonceReplayGuardType.IN_MEMORY, NonceReplayGuardType.select("in-memory", true));
    // 非 dev/test 显式要求 in-memory 必须启动失败（fail-closed），不得静默装配无界内存
    assertThrows(
        IllegalStateException.class, () -> NonceReplayGuardType.select("in-memory", false));
  }

  @Test
  void missing_config_does_not_fallback_to_unsafe_in_memory() {
    // 配置缺失：dev/test 默认 in-memory（便利），非 dev/test 默认 jdbc（安全）
    assertEquals(NonceReplayGuardType.IN_MEMORY, NonceReplayGuardType.select(null, true));
    assertEquals(NonceReplayGuardType.IN_MEMORY, NonceReplayGuardType.select("", true));
    assertEquals(NonceReplayGuardType.JDBC, NonceReplayGuardType.select(null, false));
    assertEquals(NonceReplayGuardType.JDBC, NonceReplayGuardType.select("   ", false));
  }

  @Test
  void unknown_guard_type_fails_closed() {
    assertThrows(IllegalStateException.class, () -> NonceReplayGuardType.select("redis", true));
    assertThrows(IllegalStateException.class, () -> NonceReplayGuardType.select("redis", false));
  }

  @Test
  void isDevOrTest_detects_dev_or_test_profiles_only() {
    assertTrue(NonceReplayGuardType.isDevOrTest(List.of("dev")));
    assertTrue(NonceReplayGuardType.isDevOrTest(List.of("test")));
    assertTrue(NonceReplayGuardType.isDevOrTest(List.of("prod", "test")));
    assertTrue(NonceReplayGuardType.isDevOrTest(Set.of("DEV")));

    assertFalse(NonceReplayGuardType.isDevOrTest(List.of("prod")));
    assertFalse(NonceReplayGuardType.isDevOrTest(List.of()));
    assertFalse(NonceReplayGuardType.isDevOrTest(null));
  }
}
