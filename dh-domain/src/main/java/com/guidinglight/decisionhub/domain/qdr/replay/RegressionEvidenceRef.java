package com.guidinglight.decisionhub.domain.qdr.replay;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * QDR regression evidence ref。
 *
 * <p>该对象只保存可审计 ref、hash 和用途角色，不保存 raw prompt、raw provider response、
 * credential 或任何交易执行 payload。它用于 B3 comparator finding 与 persistence command 的
 * evidenceRef 字段，不能被解释为 trading signal。
 *
 * @param refType     ref 类型，例如 `MOCK_GATEWAY_SUMMARY`。
 * @param refId       租户内安全 ref ID。
 * @param contentHash ref 指向内容的 SHA-256 hash。
 * @param role        ref 在 regression 中的用途角色。
 */
public record RegressionEvidenceRef(
        String refType,
        String refId,
        String contentHash,
        String role) {

    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern RAW_OR_SECRET =
            Pattern.compile(
                    "(?i)(rawPrompt|raw_prompt|promptText|rawProviderResponse|"
                            + "raw_provider_response|providerRaw|credential|apiKey|apiSecret|"
                            + "passphrase|token|cookie|secret)");
    private static final Pattern EXECUTABLE_ACTION =
            Pattern.compile("\\b(BUY|SELL|MARKET_ORDER|PLACE_ORDER|CANCEL_ORDER|MUTATE_NQ_STATE)\\b");

    /**
     * 校验 regression evidence ref 只包含安全引用。
     */
    public RegressionEvidenceRef {
        refType = requireText(refType, "refType");
        refId = requireText(refId, "refId");
        contentHash = requireHash(contentHash, "contentHash");
        role = requireText(role, "role");
    }

    /**
     * 转成 finding/evidence 字段使用的短 ref。
     *
     * @return 不含原始内容的 evidence ref。
     */
    public String compactRef() {
        return refType + ":" + refId + ":" + contentHash.substring(0, 16);
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (RAW_OR_SECRET.matcher(checked).find()) {
            throw new IllegalArgumentException(field + " rejected by redaction guard");
        }
        if (EXECUTABLE_ACTION.matcher(checked).find()) {
            throw new IllegalArgumentException(field + " rejected by trading-term guard");
        }
        return checked;
    }

    private static String requireHash(final String value, final String field) {
        final String checked = requireText(value, field).toLowerCase();
        if (!SHA_256_HEX.matcher(checked).matches()) {
            throw new IllegalArgumentException(field + " must be SHA-256 hex");
        }
        return checked;
    }
}
