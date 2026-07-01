package com.guidinglight.decisionhub.usecase.decision.support;

import com.guidinglight.decisionhub.domain.decision.DecisionRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * K6 mock NQ dry-run fixture 读取器。
 *
 * <p>fixture 只作为 contract test 输入样例，不做 K7 eval，不包含真实账户、凭证、订单或交易指令。测试中实际运行仍使用
 * `MockNqDecisionRequestFactory` 构造 domain object，避免在 usecase 测试模块新增 JSON 解析依赖。
 */
public final class MockNqDryRunFixtures {

    /**
     * 有效 mock NQ dry-run 请求样例。
     */
    public static final String VALID_DRYRUN = "mock_nq_valid_dryrun.json";

    /**
     * provider guard blocked dry-run 请求样例。
     */
    public static final String PROVIDER_BLOCKED = "mock_nq_provider_blocked.json";

    /**
     * no-live-trade guard 请求样例。
     */
    public static final String NO_LIVE_TRADE_GUARD = "mock_nq_no_live_trade_guard.json";

    private static final Path FIXTURE_ROOT =
            Path.of("..", "golden_cases", "decision").toAbsolutePath().normalize();

    private MockNqDryRunFixtures() {
    }

    /**
     * 返回 K6 本轮允许新增的全部 fixture 文件名。
     *
     * @return fixture 文件名列表。
     */
    public static List<String> fixtureFilenames() {
        return List.of(VALID_DRYRUN, PROVIDER_BLOCKED, NO_LIVE_TRADE_GUARD);
    }

    /**
     * 读取 fixture 文本，用于 contract test 检查字段与禁止词。
     *
     * @param filename fixture 文件名。
     * @return UTF-8 fixture 内容。
     */
    public static String readFixture(final String filename) {
        try {
            return Files.readString(FIXTURE_ROOT.resolve(filename));
        } catch (final IOException error) {
            throw new AssertionError("failed to read mock NQ dry-run fixture: " + filename, error);
        }
    }

    /**
     * 返回与 `mock_nq_valid_dryrun.json` 对齐的 domain request。
     *
     * @return mock NQ 有效 dry-run 请求。
     */
    public static DecisionRequest validDryRunRequest() {
        return MockNqDecisionRequestFactory.validDryRunRequest();
    }

    /**
     * 返回与 `mock_nq_provider_blocked.json` 对齐的 domain request。
     *
     * @return provider guard blocked 场景请求。
     */
    public static DecisionRequest providerBlockedRequest() {
        return MockNqDecisionRequestFactory.providerBlockedRequest();
    }

    /**
     * 返回与 `mock_nq_no_live_trade_guard.json` 对齐的 domain request。
     *
     * @return no-live-trade guard 场景请求。
     */
    public static DecisionRequest noLiveTradeGuardRequest() {
        return MockNqDecisionRequestFactory.noLiveTradeGuardRequest();
    }
}
