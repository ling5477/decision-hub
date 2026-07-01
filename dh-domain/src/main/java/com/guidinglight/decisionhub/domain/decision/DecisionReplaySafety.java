package com.guidinglight.decisionhub.domain.decision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** K4 replay 只读视图的内部校验工具，统一处理空值、只读集合和可审计 ID。 */
final class DecisionReplaySafety {

  private DecisionReplaySafety() {}

  static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }

  static Map<String, Object> copyMap(final Map<String, Object> source) {
    if (source == null || source.isEmpty()) {
      return Map.of();
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }

  static List<String> copyStrings(final List<String> source) {
    if (source == null || source.isEmpty()) {
      return List.of();
    }
    return List.copyOf(source);
  }

  static <T> List<T> copyList(final List<T> source) {
    if (source == null || source.isEmpty()) {
      return List.of();
    }
    return Collections.unmodifiableList(new ArrayList<>(source));
  }
}
