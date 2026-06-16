package com.guidinglight.decisionhub.integration0.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Int0RequestFactory 是 test-only 请求构造工具。
 *
 * <p>从 classpath fixture 加载脱敏 JSON，并基于 test-only 假 secret 生成合法签名 header，让负向用例只需修改单一要素。 所有 tenant / source /
 * requestId / traceId / nonce 都是固定假值。
 */
public final class Int0RequestFactory {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Int0RequestFactory() {}

  public static JsonNode loadFixture(String name) {
    String resource = "/integration0/" + name;
    try (InputStream in = Int0RequestFactory.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("fixture not found on classpath: " + resource);
      }
      return MAPPER.readTree(in);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static String toJson(JsonNode node) {
    try {
      return MAPPER.writeValueAsString(node);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static JsonNode parse(String json) {
    try {
      return MAPPER.readTree(json);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  /**
   * 构造一组合法签名 header（nonce 可指定），供 happy path 与负向变体使用。
   *
   * <p>timestamp canonical 为 RFC3339 / ISO-8601 UTC `Z`（由 {@code nowEpochSeconds} 派生：
   * {@code Instant.ofEpochSecond(nowEpochSeconds).toString()}，例 {@code 2026-06-15T12:34:56Z}），与 DH 生产
   * {@code Instant.parse} 实际接受格式一致；**不再使用 epoch 秒**。{@code nowEpochSeconds} 仅作 test-only 参考时钟，
   * 窗口比较仍以秒为单位（见 {@link Int0ContractValidator}）。
   */
  public static Map<String, String> validHeaders(String body, long nowEpochSeconds, String nonce) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(Int0Contract.H_SOURCE, Int0Contract.FAKE_SOURCE);
    headers.put(Int0Contract.H_TENANT, Int0Contract.FAKE_TENANT);
    headers.put(Int0Contract.H_REQUEST_ID, "req-int0-0001");
    headers.put(Int0Contract.H_TRACE_ID, "trace-int0-0001");
    headers.put(Int0Contract.H_TIMESTAMP, Instant.ofEpochSecond(nowEpochSeconds).toString());
    headers.put(Int0Contract.H_NONCE, nonce);
    headers.put(Int0Contract.H_CONTENT_TYPE, Int0Contract.CONTENT_TYPE_JSON);
    String signature =
        Int0Signing.hmacSha256Hex(Int0Signing.canonical(headers, body), Int0Contract.FAKE_SECRET);
    headers.put(Int0Contract.H_SIGNATURE, signature);
    return headers;
  }
}
