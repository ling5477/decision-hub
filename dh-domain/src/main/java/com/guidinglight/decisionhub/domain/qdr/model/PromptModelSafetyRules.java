package com.guidinglight.decisionhub.domain.qdr.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * stage-qdr-3 B1 prompt/model 本地安全规则。
 *
 * <p>本类只做 deterministic 字符串校验与 JDK SHA-256 checksum，不调用 provider、不访问 HTTP、
 * 不读取凭证，也不保存 raw provider response。
 */
public final class PromptModelSafetyRules {

    private static final Pattern SECRET_LIKE =
            Pattern.compile(
                    "(?i)(api[_-]?key|api[_-]?secret|passphrase|credential|token|cookie|"
                            + "private[_ -]?key|mnemonic|password|secret)");

    private static final List<Pattern> EXECUTABLE_TRADING_INSTRUCTIONS =
            List.of(
                    Pattern.compile("\\bBUY\\b"),
                    Pattern.compile("\\bSELL\\b"),
                    Pattern.compile("\\bPLACE_ORDER\\b"),
                    Pattern.compile("\\bCANCEL_ORDER\\b"),
                    Pattern.compile("\\bMARKET_ORDER\\b"),
                    Pattern.compile("\\bLIMIT_ORDER\\b"),
                    Pattern.compile("(?i)\\b(place|execute|submit|cancel)\\s+order\\b"),
                    Pattern.compile("(?i)\\bmutate\\s+nq\\s+state\\b"));

    private PromptModelSafetyRules() {
    }

    /**
     * 校验必填文本并返回 trim 后的值。
     *
     * @param value 待校验文本。
     * @param field 字段名，只用于固定错误消息。
     * @return trim 后的文本。
     */
    public static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }

    /**
     * 校验 prompt 文本不为空、不含疑似密钥、不含可执行交易指令。
     *
     * @param value prompt template body。
     * @return trim 后的 prompt body。
     */
    public static String requireSafePromptBody(final String value) {
        final String checked = requireText(value, "templateBody");
        rejectSecretLike("prompt content", checked, PromptValidationException::secretLikeMaterial);
        rejectExecutableTradingInstruction("prompt content", checked);
        return checked;
    }

    /**
     * 校验 model/provider profile 文本不包含疑似密钥。
     *
     * @param field 字段名。
     * @param value 字段值。
     * @return trim 后的字段值。
     */
    public static String requireSafeProfileText(final String field, final String value) {
        final String checked = requireText(value, field);
        rejectSecretLike(field, checked, ModelVersionValidationException::secretLikeMaterial);
        return checked;
    }

    /**
     * 对可选 profile 字段做脱敏安全校验。
     *
     * @param field 字段名。
     * @param value 字段值。
     * @return null 或 trim 后的安全文本。
     */
    public static String optionalSafeProfileText(final String field, final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireSafeProfileText(field, value);
    }

    /**
     * 计算 deterministic SHA-256 checksum。
     *
     * @param parts 参与 checksum 的有序字段。
     * @return 64 位 hex SHA-256。
     */
    public static String sha256Hex(final List<String> parts) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                digest.update(normalize(part).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (final NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    /**
     * 判断文本是否包含疑似密钥材料。
     *
     * @param value 待检查文本。
     * @return true 表示包含疑似密钥。
     */
    public static boolean containsSecretLikeMaterial(final String value) {
        return value != null && SECRET_LIKE.matcher(value).find();
    }

    /**
     * 判断文本是否包含可执行交易指令。
     *
     * @param value 待检查文本。
     * @return true 表示包含交易执行意图。
     */
    public static boolean containsExecutableTradingInstruction(final String value) {
        if (value == null) {
            return false;
        }
        return EXECUTABLE_TRADING_INSTRUCTIONS.stream().anyMatch(pattern -> pattern.matcher(value).find());
    }

    private static void rejectSecretLike(
            final String field, final String value, final SafeExceptionFactory exceptionFactory) {
        if (containsSecretLikeMaterial(value)) {
            throw exceptionFactory.create(field);
        }
    }

    private static void rejectExecutableTradingInstruction(final String field, final String value) {
        if (containsExecutableTradingInstruction(value)) {
            throw PromptValidationException.executableTradingInstruction(field);
        }
    }

    private static String normalize(final String value) {
        return Objects.requireNonNull(value, "checksum part").replace("\r\n", "\n");
    }

    @FunctionalInterface
    private interface SafeExceptionFactory {
        RuntimeException create(String field);
    }
}
