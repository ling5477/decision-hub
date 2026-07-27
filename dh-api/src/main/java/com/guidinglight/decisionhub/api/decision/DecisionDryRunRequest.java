package com.guidinglight.decisionhub.api.decision;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/**
 * `POST /api/ai/decision-dry-runs` 的入站 request envelope。
 *
 * <p>该 DTO 只描述 wire-level 输入，不做业务授权。Controller 会在绑定后继续校验 HMAC、timestamp、nonce、
 * tenant/source allowlist、payload cap、forbidden material、feature flag 和 audit fail-closed。
 *
 * @param requestId 请求 ID。
 * @param traceId trace ID。
 * @param tenantId tenant ID；必须与认证上下文一致。
 * @param source 来源；首批仅允许 dev/test 的 NQ_DRYRUN。
 * @param environment 显式 signed 环境；仅允许 DEV 或 TEST，不能由 profile 或服务器推断。
 * @param timestamp body timestamp；必须与 canonical timestamp header 一致。
 * @param nonce body nonce；必须与 canonical nonce header 一致。
 * @param schemaVersion schema version。
 * @param dryRun 必须为 true。
 * @param decisionContext 只读决策上下文。
 * @param forbiddenCapabilities 调用方声明的禁用能力集合。
 */
public record DecisionDryRunRequest(
    String requestId,
    String traceId,
    String tenantId,
    String source,
    String environment,
    String timestamp,
    String nonce,
    String schemaVersion,
    Boolean dryRun,
    JsonNode decisionContext,
    Set<String> forbiddenCapabilities) {}
