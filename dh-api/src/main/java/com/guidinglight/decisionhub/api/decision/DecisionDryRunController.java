package com.guidinglight.decisionhub.api.decision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.api.TraceIdFilter;
import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.NormalizedNqDhHeaders;
import com.guidinglight.decisionhub.security.nq.NqDhHeaderParser;
import com.guidinglight.decisionhub.security.nq.NqDhHeaderValidationResult;
import com.guidinglight.decisionhub.security.nq.NqDhHeaderValidator;
import com.guidinglight.decisionhub.security.nq.NqDryRunAuthRequest;
import com.guidinglight.decisionhub.security.nq.NqDryRunAuthResult;
import com.guidinglight.decisionhub.security.nq.RateLimitResult;
import com.guidinglight.decisionhub.security.nq.RateLimiter;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunCommand;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunContext;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunErrorCode;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunResult;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration-1 limited dry-run inbound endpoint。
 *
 * <p>本 Controller 只接收 signed / timestamped / nonce / tenant-bound request，并委托现有
 * DecisionOrchestrator 生成 read-only snapshot。它不创建 NQ client、不发出 outbound HTTP、不接真实 provider、
 * 不接 Agent/LangGraph runtime、不启用 LIVE，不返回可执行交易指令。
 */
@RestController
@RequestMapping("/api/ai")
public final class DecisionDryRunController {

  private static final String ENDPOINT_PATH = "/api/ai/decision-dry-runs";
  private static final String ROUTE = "DECISION_DRY_RUN";
  private static final String ERROR = "DRY_RUN_REJECTED";

  private static final Set<String> FORBIDDEN_FIELD_NAMES =
      Set.of(
          "credential",
          "apikey",
          "apisecret",
          "passphrase",
          "accountsecret",
          "executableorder",
          "orderid",
          "accountid",
          "quantity",
          "leverage",
          "orderprice",
          "price");

  private static final Set<String> FORBIDDEN_TEXT_VALUES =
      Set.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER");

  private final NqDhHeaderParser headerParser = new NqDhHeaderParser();
  private final NqDhHeaderValidator headerValidator = new NqDhHeaderValidator();
  private final DecisionDryRunService dryRunService;
  private final HmacNqDryRunAuthenticator dryRunAuthenticator;
  private final RateLimiter rateLimiter;
  private final ObjectMapper objectMapper;

  /**
   * 创建 dry-run Controller。
   *
   * @param dryRunService dry-run usecase。
   * @param dryRunAuthenticator dry-run HMAC authenticator。
   * @param rateLimiter 入站限流器。
   * @param objectMapper JSON mapper。
   */
  public DecisionDryRunController(
      final DecisionDryRunService dryRunService,
      final HmacNqDryRunAuthenticator dryRunAuthenticator,
      final RateLimiter rateLimiter,
      final ObjectMapper objectMapper) {
    this.dryRunService = Objects.requireNonNull(dryRunService, "dryRunService");
    this.dryRunAuthenticator =
        Objects.requireNonNull(dryRunAuthenticator, "dryRunAuthenticator");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  /**
   * 执行一次 limited dry-run decision。
   *
   * @param rawBody 原始 JSON body，用于 HMAC body hash。
   * @param httpRequest HTTP request。
   * @return 成功 snapshot 或 fail-closed error envelope。
   */
  @PostMapping("/decision-dry-runs")
  public ResponseEntity<Object> decide(
      @RequestBody(required = false) final String rawBody, final HttpServletRequest httpRequest) {
    DecisionDryRunCommand command = null;
    final String httpTraceId = resolveHttpTraceId(httpRequest);
    try {
      final String tenantId = AuthenticatedRequest.requireTenantId(httpRequest);
      if (dryRunAuthenticator.isPayloadTooLarge(rawBody, httpRequest.getContentLengthLong())) {
        return toResponse(
            dryRunService.reject(
                null,
                413,
                DecisionDryRunErrorCode.PAYLOAD_TOO_LARGE,
                "dry-run request payload exceeds configured cap"),
            httpTraceId);
      }
      final ParsedRequest parsed = parse(rawBody);
      command = parsed.command();
      final NormalizedNqDhHeaders headers = headerParser.parseCanonical(httpRequest::getHeader);

      final DecisionDryRunResult bodyHeaderBinding = validateBodyHeaderBinding(command, headers);
      if (bodyHeaderBinding != null) {
        return toResponse(bodyHeaderBinding, httpTraceId);
      }

      final RateLimitResult rateLimit =
          rateLimiter.check(headers.source(), tenantId, ROUTE, TimeProvider.now());
      if (!rateLimit.allowed()) {
        return toResponse(
            dryRunService.reject(
                command,
                429,
                DecisionDryRunErrorCode.RATE_LIMITED,
                "dry-run request rate limited"),
            httpTraceId);
      }

      final NqDhHeaderValidationResult headerBinding =
          headerValidator.validate(headers, tenantId, parsed.request().requestId(), parsed.request().traceId());
      if (!headerBinding.valid()) {
        return toResponse(
            dryRunService.reject(
                command,
                403,
                DecisionDryRunErrorCode.TENANT_MISMATCH,
                "dry-run header binding mismatch"),
            httpTraceId);
      }

      final NqDryRunAuthResult authResult =
          dryRunAuthenticator.authenticate(
              new NqDryRunAuthRequest(
                  "POST",
                  ENDPOINT_PATH,
                  headers.source(),
                  parsed.request().source(),
                  tenantId,
                  parsed.request().tenantId(),
                  headers.timestamp(),
                  headers.nonce(),
                  headers.signature(),
                  parsed.request().requestId(),
                  parsed.request().traceId(),
                  parsed.request().schemaVersion(),
                  parsed.rawBody(),
                  httpRequest.getContentLengthLong(),
                  TimeProvider.now()));
      if (!authResult.allowed()) {
        return toResponse(
            dryRunService.reject(
                command,
                authResult.status(),
                DecisionDryRunErrorCode.valueOf(authResult.errorCode()),
                authResult.reason()),
            httpTraceId);
      }

      return toResponse(dryRunService.execute(command), httpTraceId);
    } catch (final ErrorResponseException error) {
      return toResponse(
          dryRunService.reject(
              command,
              error.getStatusCode().value(),
              DecisionDryRunErrorCode.POLICY_DENIED,
              "dry-run authentication context missing"),
          httpTraceId);
    } catch (final JsonProcessingException error) {
      return toResponse(
          dryRunService.reject(
              command,
              403,
              DecisionDryRunErrorCode.POLICY_DENIED,
              "dry-run request json invalid"),
          httpTraceId);
    } catch (final RuntimeException error) {
      return toResponse(
          dryRunService.reject(
              command,
              500,
              DecisionDryRunErrorCode.UNKNOWN_ERROR,
              "dry-run request failed closed"),
          httpTraceId);
    }
  }

  private DecisionDryRunResult validateBodyHeaderBinding(
      final DecisionDryRunCommand command, final NormalizedNqDhHeaders headers) {
    if (!same(command.timestamp(), headers.timestamp())) {
      return dryRunService.reject(
          command,
          401,
          DecisionDryRunErrorCode.TIMESTAMP_INVALID,
          "body timestamp does not match canonical header");
    }
    if (!same(command.nonce(), headers.nonce())) {
      return dryRunService.reject(
          command,
          401,
          DecisionDryRunErrorCode.SIGNATURE_INVALID,
          "body nonce does not match canonical header");
    }
    return null;
  }

  private ParsedRequest parse(final String rawBody) throws JsonProcessingException {
    final String body = rawBody == null ? "" : rawBody;
    final JsonNode root = objectMapper.readTree(body);
    final DecisionDryRunRequest request = objectMapper.treeToValue(root, DecisionDryRunRequest.class);
    final boolean forbiddenMaterialDetected = containsForbiddenMaterial(root, false);
    return new ParsedRequest(body, request, toCommand(request, forbiddenMaterialDetected));
  }

  private DecisionDryRunCommand toCommand(
      final DecisionDryRunRequest request, final boolean forbiddenMaterialDetected) {
    final JsonNode context = request == null ? null : request.decisionContext();
    return new DecisionDryRunCommand(
        request == null ? null : request.requestId(),
        request == null ? null : request.traceId(),
        request == null ? null : request.tenantId(),
        request == null ? null : request.source(),
        request == null ? null : request.timestamp(),
        request == null ? null : request.nonce(),
        request == null ? null : request.schemaVersion(),
        request != null && Boolean.TRUE.equals(request.dryRun()),
        request == null ? Set.of() : request.forbiddenCapabilities(),
        toContext(context),
        forbiddenMaterialDetected);
  }

  private DecisionDryRunContext toContext(final JsonNode context) {
    if (context == null || context.isMissingNode() || context.isNull()) {
      return null;
    }
    final JsonNode subject = context.path("subject");
    final JsonNode snapshot = context.path("contextSnapshot");
    return new DecisionDryRunContext(
        text(subject, "symbol", text(context, "symbol", null)),
        text(subject, "market", text(context, "market", null)),
        text(subject, "timeframe", text(context, "timeframe", null)),
        text(subject, "strategyRef", text(context, "strategyRef", null)),
        text(subject, "researchRef", text(context, "researchRef", null)),
        text(context, "contextRef", null),
        text(snapshot, "snapshotId", text(context, "snapshotId", null)),
        parseInstant(text(snapshot, "capturedAt", text(context, "capturedAt", null))),
        textList(snapshot.path("evidenceRefs").isMissingNode()
            ? context.path("evidenceRefs")
            : snapshot.path("evidenceRefs")),
        context.toString().getBytes(StandardCharsets.UTF_8).length);
  }

  private ResponseEntity<Object> toResponse(
      final DecisionDryRunResult result, final String httpTraceId) {
    final String traceId = firstNonBlank(httpTraceId, result.traceId());
    if (result.success()) {
      final DecisionDryRunSnapshot snapshot = result.snapshot();
      return ResponseEntity.status(result.status())
          .header(TraceIdFilter.TRACE_HEADER, firstNonBlank(traceId, "unknown-trace"))
          .body(
              (Object)
                  new DecisionDryRunSuccessResponse(
                      snapshot.decisionId(),
                      snapshot.dryRun(),
                      snapshot.action(),
                      snapshot.confidence(),
                      snapshot.riskLevel(),
                      snapshot.reasons(),
                      snapshot.traceSummary(),
                      snapshot.replayRef(),
                      snapshot.auditRef(),
                      snapshot.schemaVersion()));
    }
    return ResponseEntity.status(result.status())
        .header(TraceIdFilter.TRACE_HEADER, firstNonBlank(traceId, "unknown-trace"))
        .body(
            (Object)
                new DecisionDryRunErrorResponse(
                    ERROR,
                    result.errorCode().name(),
                    result.message(),
                    result.requestId(),
                    result.traceId(),
                    result.auditRef()));
  }

  private static boolean containsForbiddenMaterial(final JsonNode node, final boolean capabilityNode) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return false;
    }
    if (node.isObject()) {
      final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        final Map.Entry<String, JsonNode> field = fields.next();
        final String normalized = normalizeField(field.getKey());
        final boolean inCapabilities = capabilityNode || "forbiddencapabilities".equals(normalized);
        if (!inCapabilities && FORBIDDEN_FIELD_NAMES.contains(normalized)) {
          return true;
        }
        if (containsForbiddenMaterial(field.getValue(), inCapabilities)) {
          return true;
        }
      }
      return false;
    }
    if (node.isArray()) {
      for (final JsonNode item : node) {
        if (containsForbiddenMaterial(item, capabilityNode)) {
          return true;
        }
      }
      return false;
    }
    if (!capabilityNode && node.isTextual()) {
      return FORBIDDEN_TEXT_VALUES.contains(node.asText().trim().toUpperCase(Locale.ROOT));
    }
    return false;
  }

  private static String text(final JsonNode node, final String field, final String fallback) {
    if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
      return fallback;
    }
    final String value = node.path(field).asText();
    return value == null || value.isBlank() ? fallback : value;
  }

  private static List<String> textList(final JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    final Set<String> values = new HashSet<>();
    for (final JsonNode item : node) {
      if (item.isTextual() && !item.asText().isBlank()) {
        values.add(item.asText());
      }
    }
    return List.copyOf(values);
  }

  private static Instant parseInstant(final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (final RuntimeException ignored) {
      return null;
    }
  }

  private static String resolveHttpTraceId(final HttpServletRequest httpRequest) {
    final Object attr = httpRequest.getAttribute(TraceIdFilter.TRACE_HEADER);
    return attr == null ? null : attr.toString();
  }

  private static boolean same(final String a, final String b) {
    return a != null && b != null && a.equals(b);
  }

  private static String firstNonBlank(final String first, final String second) {
    return first == null || first.isBlank() ? second : first;
  }

  private static String normalizeField(final String field) {
    return field == null
        ? ""
        : field.replace("_", "").replace("-", "").trim().toLowerCase(Locale.ROOT);
  }

  private record ParsedRequest(
      String rawBody, DecisionDryRunRequest request, DecisionDryRunCommand command) {}
}
