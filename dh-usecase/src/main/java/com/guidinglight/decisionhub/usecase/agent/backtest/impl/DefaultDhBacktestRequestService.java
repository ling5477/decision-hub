package com.guidinglight.decisionhub.usecase.agent.backtest.impl;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitResult;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitStatus;
import com.guidinglight.decisionhub.connector.nq.fake.DisabledNqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.fake.FakeNqBacktestClient;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestCommand;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestErrorCode;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestRepository;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestResult;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.inmemory.InMemoryDhBacktestRequestRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stage3-B3：{@link DhBacktestRequestService} 默认实现。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §3.1 / §4 / §5 / §7 / §8。
 *
 * <p>核心流程（参见 SPEC §4 状态机）：
 *
 * <ol>
 *   <li>校验 Command（symbols / dates / capital / frequency / paramsHash）；
 *       失败 -&gt; FAILED + DH_VALIDATION_FAILED；
 *   <li>计算 paramsHash = sha256(candidateId || strategyVersion || strategyParametersJson ||
 *       startDate || endDate || initialCapital || symbols.sorted || frequency)；
 *   <li>24h 内同 (candidateId + paramsHash) 命中 -&gt; IDEMPOTENT_SHORT_CIRCUIT；
 *   <li>否则生成 requestId(UUIDv7-style)，构造 DhBacktestRequest(draft)，写仓储；
 *   <li>调用 NqBacktestClient.submit(typed)，映射 NQ status -&gt; outcome：
 *       <ul>
 *         <li>ACCEPTED -&gt; ACCEPTED 或 FAKE_ACCEPTED (Fake 路径自动识别)；状态机 SUBMITTED -&gt; ACCEPTED
 *         <li>DUPLICATE -&gt; DUPLICATE；状态机切 ACCEPTED
 *         <li>DISABLED -&gt; DISABLED；状态机保持 QUEUED（service 标记为 DISABLED outcome）
 *         <li>FAILED -&gt; FAILED；状态机 FAILED
 *       </ul>
 *   <li>更新仓储 + 返回 typed Result（不抛系统异常）。
 * </ol>
 *
 * <p>硬约束：不允许触发 ResearchRun 主流程失败；不允许在 service 内自动生成 RESULT_READY。
 */
public final class DefaultDhBacktestRequestService implements DhBacktestRequestService {

  private final DhBacktestRequestRepository repository;
  private final NqBacktestClient nqBacktestClient;
  private final Clock clock;

  public DefaultDhBacktestRequestService(
      final DhBacktestRequestRepository repository, final NqBacktestClient nqBacktestClient) {
    this(repository, nqBacktestClient, Clock.systemUTC());
  }

  public DefaultDhBacktestRequestService(
      final DhBacktestRequestRepository repository,
      final NqBacktestClient nqBacktestClient,
      final Clock clock) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.nqBacktestClient = Objects.requireNonNull(nqBacktestClient, "nqBacktestClient");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public DhBacktestRequestResult submit(final DhBacktestRequestCommand command) {
    Objects.requireNonNull(command, "command");

    // 1) 校验
    final String validationError = validate(command);
    if (validationError != null) {
      // 校验失败前无 requestId；用临时占位 UUID，便于 caller 关联日志，不入仓储
      return DhBacktestRequestResult.failed(
          IdGenerator.newId(), DhBacktestRequestErrorCode.DH_VALIDATION_FAILED, validationError);
    }

    // 2) 计算 paramsHash
    final String paramsHash = computeParamsHash(command);

    // 3) 24h 短路
    final Instant now = clock.instant();
    final var existing =
        repository.findByCandidateAndParamsHashWithin24h(command.getCandidateId(), paramsHash, now);
    if (existing.isPresent()) {
      final var snap = existing.get();
      return DhBacktestRequestResult.idempotentShortCircuit(
          snap.getRequestId(), snap.getStatus(), snap.getJobId(), snap.getAcceptedAt());
    }

    // 4) 构造 DhBacktestRequest + 写仓储
    final String requestId = IdGenerator.newId();
    final DhBacktestRequest request =
        DhBacktestRequest.draft(
            requestId,
            command.getTraceId(),
            command.getCandidateId(),
            command.getStrategyName(),
            command.getStrategyVersion(),
            command.getStrategyParametersJson(),
            null,
            null,
            command.getStartDate(),
            command.getEndDate(),
            command.getInitialCapital(),
            command.getSymbols(),
            command.getFrequency(),
            command.getRequestedBy(),
            now);
    if (repository instanceof InMemoryDhBacktestRequestRepository inMem) {
      inMem.saveWithParamsHash(request, paramsHash);
    } else {
      // 端口默认路径：JDBC 实现需在 save 时自行计算/接受 paramsHash；
      // Stage3-B3 默认 InMemory 装配，本分支只在自定义实现下触发
      repository.save(request);
    }

    // 5) 调 NQ client
    final boolean fakeMode = nqBacktestClient instanceof FakeNqBacktestClient;
    final boolean disabledMode = nqBacktestClient instanceof DisabledNqBacktestClient;
    final NqBacktestSubmitResult clientResult;
    try {
      clientResult = nqBacktestClient.submit(request);
    } catch (final RuntimeException e) {
      // 任何意外异常都映射到 FAILED；不让 caller 看到 RuntimeException
      repository.updateAfterSubmit(
          requestId,
          DhBacktestRequestStatus.FAILED,
          null,
          null,
          DhBacktestRequestErrorCode.NETWORK.name(),
          e.getClass().getSimpleName());
      return DhBacktestRequestResult.failed(
          requestId, DhBacktestRequestErrorCode.NETWORK, e.getClass().getSimpleName());
    }
    if (clientResult == null) {
      repository.updateAfterSubmit(
          requestId,
          DhBacktestRequestStatus.FAILED,
          null,
          null,
          DhBacktestRequestErrorCode.PROTOCOL_VIOLATION.name(),
          "null client result");
      return DhBacktestRequestResult.failed(
          requestId, DhBacktestRequestErrorCode.PROTOCOL_VIOLATION, "null client result");
    }

    // 6) 映射 status -> outcome + 更新仓储
    return mapClientResult(requestId, clientResult, fakeMode, disabledMode);
  }

  private DhBacktestRequestResult mapClientResult(
      final String requestId,
      final NqBacktestSubmitResult clientResult,
      final boolean fakeMode,
      final boolean disabledMode) {
    final NqBacktestSubmitStatus status = clientResult.getStatus();
    switch (status) {
      case ACCEPTED:
        repository.updateAfterSubmit(
            requestId,
            DhBacktestRequestStatus.ACCEPTED,
            clientResult.getJobId(),
            clientResult.getAcceptedAt(),
            null,
            null);
        return DhBacktestRequestResult.accepted(
            requestId, clientResult.getJobId(), clientResult.getAcceptedAt(), fakeMode);
      case DUPLICATE:
        repository.updateAfterSubmit(
            requestId,
            DhBacktestRequestStatus.ACCEPTED,
            clientResult.getJobId(),
            clientResult.getAcceptedAt(),
            DhBacktestRequestErrorCode.DUPLICATE_REQUEST.name(),
            null);
        return DhBacktestRequestResult.duplicate(
            requestId, clientResult.getJobId(), clientResult.getAcceptedAt());
      case DISABLED:
        final DhBacktestRequestErrorCode disabledCode =
            disabledMode
                ? DhBacktestRequestErrorCode.DH_DISABLED
                : DhBacktestRequestErrorCode.NQ_AI_DISABLED;
        repository.updateAfterSubmit(
            requestId,
            DhBacktestRequestStatus.QUEUED,
            null,
            null,
            disabledCode.name(),
            null);
        return DhBacktestRequestResult.disabled(requestId, disabledCode);
      case FAILED:
      default:
        repository.updateAfterSubmit(
            requestId,
            DhBacktestRequestStatus.FAILED,
            null,
            null,
            clientResult.getErrorCode(),
            clientResult.getErrorMessage());
        return DhBacktestRequestResult.failed(
            requestId, DhBacktestRequestErrorCode.HTTP_5XX, clientResult.getErrorMessage());
    }
  }

  /**
   * 入参校验。返回 null 表示通过，否则返回失败原因。
   *
   * <p>注意：Command builder 已对 null 必填字段 NPE 兜底；本方法补充业务级校验。
   */
  private static String validate(final DhBacktestRequestCommand c) {
    if (c.getSymbols().isEmpty()) {
      return "symbols must not be empty";
    }
    if (c.getEndDate().isBefore(c.getStartDate())) {
      return "endDate must not be before startDate";
    }
    if (c.getInitialCapital() <= 0.0) {
      return "initialCapital must be positive";
    }
    return null;
  }

  /**
   * 参数哈希。参与字段：candidateId / strategyVersion / strategyParametersJson / startDate / endDate /
   * initialCapital / symbols(sorted) / frequency。
   */
  static String computeParamsHash(final DhBacktestRequestCommand c) {
    final List<String> sortedSymbols =
        c.getSymbols().stream().sorted().collect(Collectors.toUnmodifiableList());
    final String material =
        c.getCandidateId()
            + "|"
            + c.getStrategyVersion()
            + "|"
            + c.getStrategyParametersJson()
            + "|"
            + c.getStartDate()
            + "|"
            + c.getEndDate()
            + "|"
            + c.getInitialCapital()
            + "|"
            + String.join(",", sortedSymbols)
            + "|"
            + c.getFrequency().name();
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] hash = md.digest(material.getBytes(StandardCharsets.UTF_8));
      final StringBuilder sb = new StringBuilder(64);
      for (final byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (final NoSuchAlgorithmException e) {
      // SHA-256 在 JRE 中必定存在；理论上不会触发。兜底返回 hashCode 十六进制。
      return Integer.toHexString(material.hashCode());
    }
  }
}
