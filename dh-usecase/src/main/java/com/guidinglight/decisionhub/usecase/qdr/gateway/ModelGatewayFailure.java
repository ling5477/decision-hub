package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * Model gateway 结构化失败。
 *
 * <p>message 使用固定安全文案，不回显 raw prompt、render input、provider response 或 secret-like
 * material。
 *
 * @param code       failure code。
 * @param message    安全文案。
 * @param failClosed true 表示调用已 fail-closed。
 */
public record ModelGatewayFailure(ModelGatewayFailureCode code, String message, boolean failClosed) {

    /**
     * 校验失败模型。
     */
    public ModelGatewayFailure {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNullElse(message, code.name());
        failClosed = true;
    }

    /**
     * 创建固定文案 failure。
     *
     * @param code failure code。
     * @return fail-closed failure。
     */
    public static ModelGatewayFailure of(final ModelGatewayFailureCode code) {
        return new ModelGatewayFailure(code, safeMessage(code), true);
    }

    private static String safeMessage(final ModelGatewayFailureCode code) {
        return switch (code) {
            case MISSING_REQUIRED_FIELD -> "Required gateway identity is missing.";
            case PROMPT_VERSION_NOT_FOUND -> "Prompt version is not registered.";
            case MODEL_VERSION_NOT_FOUND -> "Model version is not registered.";
            case REGISTRY_MISMATCH -> "Prompt or model registry verification failed.";
            case PROMPT_DENIED -> "Prompt policy denied the request.";
            case PROMPT_RENDER_FAILED -> "Prompt render failed.";
            case POLICY_DENIED -> "Provider trust policy denied the request.";
            case UNKNOWN_PROVIDER -> "Provider profile is unknown.";
            case PROVIDER_DISABLED -> "Provider profile is disabled.";
            case REAL_PROVIDER_FORBIDDEN -> "Real provider is forbidden in this stage.";
            case PROVIDER_UNAVAILABLE -> "Provider is unavailable.";
            case PROVIDER_TIMEOUT -> "Provider timed out.";
            case BUDGET_EXCEEDED -> "Model call budget was exceeded.";
            case REDACTION_FAILED -> "Redaction guard rejected the request.";
            case PROVIDER_OUTPUT_INVALID -> "Provider result was invalid.";
            case UNKNOWN_ERROR -> "Gateway failed closed.";
        };
    }
}
