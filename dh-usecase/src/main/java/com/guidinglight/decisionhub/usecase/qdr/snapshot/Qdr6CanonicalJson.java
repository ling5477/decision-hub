package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * {@code QDR6-CJSON-1} canonical JSON encoder。
 *
 * <p>实现不调用对象 {@code toString()} 或默认 Map/ObjectMapper 序列化。object keys 使用 NFC 后按 Unicode
 * lexicographic order 排序，Set 按 canonical element bytes 排序，List/array 保留业务顺序；数字、时间、UUID
 * 与 JSON escape 均使用固定规则，因此不受 locale、timezone 或容器迭代顺序影响。
 */
public final class Qdr6CanonicalJson {

  /** 将冻结支持类型编码为无 BOM、无尾随换行的 UTF-8 canonical bytes。 */
  public byte[] canonicalize(final Object value) {
    try {
      final StringBuilder output = new StringBuilder();
      append(value, output);
      return output.toString().getBytes(StandardCharsets.UTF_8);
    } catch (final CanonicalReplaySnapshotAssemblyException error) {
      throw error;
    } catch (final RuntimeException error) {
      throw new CanonicalReplaySnapshotAssemblyException(
          CanonicalReplaySnapshotAssemblyException.Code.CANONICALIZATION_FAILED,
          "QDR6-CJSON-1 canonicalization failed",
          error);
    }
  }

  private void append(final Object value, final StringBuilder output) {
    if (value == null) {
      output.append("null");
    } else if (value instanceof String text) {
      appendString(text, output);
    } else if (value instanceof Character character) {
      appendString(character.toString(), output);
    } else if (value instanceof Boolean bool) {
      output.append(bool ? "true" : "false");
    } else if (value instanceof Number number) {
      output.append(canonicalNumber(number));
    } else if (value instanceof Instant instant) {
      appendString(instant.toString(), output);
    } else if (value instanceof OffsetDateTime dateTime) {
      appendString(dateTime.toInstant().toString(), output);
    } else if (value instanceof ZonedDateTime dateTime) {
      appendString(dateTime.toInstant().toString(), output);
    } else if (value instanceof UUID uuid) {
      appendString(uuid.toString().toLowerCase(java.util.Locale.ROOT), output);
    } else if (value instanceof Enum<?> enumValue) {
      appendString(enumValue.name(), output);
    } else if (value instanceof Map<?, ?> map) {
      appendMap(map, output);
    } else if (value instanceof Set<?> set) {
      appendSet(set, output);
    } else if (value instanceof Iterable<?> iterable) {
      appendIterable(iterable, output);
    } else if (value.getClass().isArray()) {
      appendArray(value, output);
    } else {
      throw new CanonicalReplaySnapshotAssemblyException(
          CanonicalReplaySnapshotAssemblyException.Code.CANONICALIZATION_FAILED,
          "unsupported canonical value type");
    }
  }

  private void appendMap(final Map<?, ?> input, final StringBuilder output) {
    final Map<String, Object> sorted = new TreeMap<>();
    for (Map.Entry<?, ?> entry : input.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new CanonicalReplaySnapshotAssemblyException(
            CanonicalReplaySnapshotAssemblyException.Code.CANONICALIZATION_FAILED,
            "canonical object keys must be strings");
      }
      final String normalized = normalize(key);
      if (sorted.containsKey(normalized)) {
        throw new CanonicalReplaySnapshotAssemblyException(
            CanonicalReplaySnapshotAssemblyException.Code.CANONICALIZATION_FAILED,
            "canonical object contains duplicate normalized keys");
      }
      sorted.put(normalized, entry.getValue());
    }
    output.append('{');
    boolean first = true;
    for (Map.Entry<String, Object> entry : sorted.entrySet()) {
      if (!first) {
        output.append(',');
      }
      first = false;
      appendString(entry.getKey(), output);
      output.append(':');
      append(entry.getValue(), output);
    }
    output.append('}');
  }

  private void appendSet(final Set<?> input, final StringBuilder output) {
    final List<byte[]> elements = new ArrayList<>();
    for (Object item : input) {
      elements.add(canonicalize(item));
    }
    elements.sort(Qdr6CanonicalJson::compareUnsigned);
    output.append('[');
    for (int index = 0; index < elements.size(); index++) {
      if (index > 0) {
        output.append(',');
      }
      output.append(new String(elements.get(index), StandardCharsets.UTF_8));
    }
    output.append(']');
  }

  private void appendIterable(final Iterable<?> values, final StringBuilder output) {
    output.append('[');
    boolean first = true;
    for (Object value : values) {
      if (!first) {
        output.append(',');
      }
      first = false;
      append(value, output);
    }
    output.append(']');
  }

  private void appendArray(final Object values, final StringBuilder output) {
    output.append('[');
    for (int index = 0; index < Array.getLength(values); index++) {
      if (index > 0) {
        output.append(',');
      }
      append(Array.get(values, index), output);
    }
    output.append(']');
  }

  private static String canonicalNumber(final Number value) {
    final BigDecimal decimal;
    if (value instanceof BigDecimal bigDecimal) {
      decimal = bigDecimal;
    } else if (value instanceof BigInteger bigInteger) {
      decimal = new BigDecimal(bigInteger);
    } else if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) {
      decimal = BigDecimal.valueOf(value.longValue());
    } else if (value instanceof Float || value instanceof Double) {
      final double floating = value.doubleValue();
      if (!Double.isFinite(floating)) {
        throw new CanonicalReplaySnapshotAssemblyException(
            CanonicalReplaySnapshotAssemblyException.Code.CANONICALIZATION_FAILED,
            "non-finite numbers are forbidden");
      }
      decimal = BigDecimal.valueOf(floating);
    } else {
      try {
        decimal = new BigDecimal(value.toString());
      } catch (final NumberFormatException error) {
        throw new CanonicalReplaySnapshotAssemblyException(
            CanonicalReplaySnapshotAssemblyException.Code.CANONICALIZATION_FAILED,
            "unsupported numeric value",
            error);
      }
    }
    if (decimal.signum() == 0) {
      return "0";
    }
    return decimal.stripTrailingZeros().toPlainString();
  }

  private static void appendString(final String value, final StringBuilder output) {
    final String normalized = normalize(value);
    output.append('"');
    for (int index = 0; index < normalized.length(); index++) {
      final char character = normalized.charAt(index);
      switch (character) {
        case '"' -> output.append("\\\"");
        case '\\' -> output.append("\\\\");
        case '\b' -> output.append("\\b");
        case '\f' -> output.append("\\f");
        case '\n' -> output.append("\\n");
        case '\r' -> output.append("\\r");
        case '\t' -> output.append("\\t");
        default -> {
          if (character < 0x20) {
            output.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) character));
          } else {
            output.append(character);
          }
        }
      }
    }
    output.append('"');
  }

  private static String normalize(final String value) {
    return Normalizer.normalize(Objects.requireNonNull(value, "value"), Normalizer.Form.NFC);
  }

  private static int compareUnsigned(final byte[] left, final byte[] right) {
    final int limit = Math.min(left.length, right.length);
    for (int index = 0; index < limit; index++) {
      final int comparison = Integer.compare(Byte.toUnsignedInt(left[index]), Byte.toUnsignedInt(right[index]));
      if (comparison != 0) {
        return comparison;
      }
    }
    return Integer.compare(left.length, right.length);
  }
}
