package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ThresholdComparatorTest {

  @Test
  void supportsFrozenOperatorsAndBoundaryEquality() {
    assertStatus("<", "9", "10", Qdr7CapacityContracts.HarnessStatus.PASS);
    assertStatus("<=", "10", "10", Qdr7CapacityContracts.HarnessStatus.PASS);
    assertStatus("=", "10", "10", Qdr7CapacityContracts.HarnessStatus.PASS);
    assertStatus(">=", "10", "10", Qdr7CapacityContracts.HarnessStatus.PASS);
    assertStatus(">", "11", "10", Qdr7CapacityContracts.HarnessStatus.PASS);
    assertStatus("<", "10", "10", Qdr7CapacityContracts.HarnessStatus.FAIL);
  }

  @Test
  void missingMetricOrUnitMismatchIsBlockedInsteadOfZeroFilled() {
    final Qdr7CapacityContracts.ThresholdResult missing =
        Qdr7CapacityContracts.compare(
            "missing", null, "<=", BigDecimal.TEN, "milliseconds", "milliseconds");
    final Qdr7CapacityContracts.ThresholdResult unitMismatch =
        Qdr7CapacityContracts.compare(
            "unit", BigDecimal.ONE, "<=", BigDecimal.TEN, "bytes", "milliseconds");

    assertThat(missing.status()).isEqualTo(Qdr7CapacityContracts.HarnessStatus.BLOCKED);
    assertThat(missing.reason()).isEqualTo("NOT_CAPTURED");
    assertThat(unitMismatch.status()).isEqualTo(Qdr7CapacityContracts.HarnessStatus.BLOCKED);
    assertThat(unitMismatch.reason()).isEqualTo("UNIT_MISMATCH");
  }

  @Test
  void unknownOperatorFailsClosed() {
    assertThatThrownBy(
            () ->
                Qdr7CapacityContracts.compare(
                    "bad", BigDecimal.ONE, "!=", BigDecimal.ONE, "count", "count"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static void assertStatus(
      final String operator,
      final String observed,
      final String threshold,
      final Qdr7CapacityContracts.HarnessStatus expected) {
    assertThat(
            Qdr7CapacityContracts.compare(
                    "criterion",
                    new BigDecimal(observed),
                    operator,
                    new BigDecimal(threshold),
                    "milliseconds",
                    "milliseconds")
                .status())
        .isEqualTo(expected);
  }
}
