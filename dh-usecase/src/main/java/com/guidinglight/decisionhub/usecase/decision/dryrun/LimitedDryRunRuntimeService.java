package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limited dry-run 的 bounded admission、queue wait 与总 deadline 执行边界。
 *
 * <p>实现使用固定最大线程数和显式 bounded queue；拒绝策略只抛出异常并映射为结构化 fail-closed 结果。不存在
 * cached thread pool、无界队列、CallerRuns、同步 fallback 或 retry。
 */
public final class LimitedDryRunRuntimeService implements DecisionDryRunService, AutoCloseable {

  private static final Duration CLOSE_WAIT = Duration.ofSeconds(1);

  private final DecisionDryRunService delegate;
  private final LimitedDryRunRuntimePolicy policy;
  private final ThreadPoolExecutor executor;
  private final AtomicBoolean closed = new AtomicBoolean();

  /**
   * 创建 bounded runtime service。非法 limits 不会创建 executor，并由 policy 在入口稳定拒绝。
   *
   * @param delegate 既有 persistent-guarded dry-run service。
   * @param policy frozen runtime policy。
   */
  public LimitedDryRunRuntimeService(
      final DecisionDryRunService delegate, final LimitedDryRunRuntimePolicy policy) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.policy = Objects.requireNonNull(policy, "policy");
    this.executor = createExecutor(policy);
  }

  @Override
  public DecisionDryRunResult execute(final DecisionDryRunCommand command) {
    final LimitedDryRunRuntimePolicy.Evaluation evaluation = policy.evaluate();
    if (!evaluation.allowed()) {
      return reject(command, evaluation.failureClassification());
    }
    if (closed.get() || executor == null) {
      return reject(command, RuntimeFailureClassification.INTERNAL_RUNTIME_FAILURE);
    }

    final long admittedAt = System.nanoTime();
    final RuntimeFuture task = new RuntimeFuture(() -> delegate.execute(command));
    try {
      executor.execute(task);
    } catch (final RejectedExecutionException rejected) {
      return reject(command, RuntimeFailureClassification.CAPACITY_REJECTED);
    }

    if (!awaitStart(task, policy.deadlinePolicy().maxQueueWait())) {
      cancelAndRemove(task);
      return reject(command, RuntimeFailureClassification.CAPACITY_REJECTED);
    }

    final long remainingNanos =
        policy.deadlinePolicy().deadline().toNanos() - (System.nanoTime() - admittedAt);
    if (remainingNanos <= 0L) {
      cancelAndRemove(task);
      return reject(command, RuntimeFailureClassification.DEADLINE_EXCEEDED);
    }
    try {
      final DecisionDryRunResult result = task.get(remainingNanos, TimeUnit.NANOSECONDS);
      final Optional<RuntimeFailureClassification> violation = policy.validateResult(result);
      return violation.map(classification -> reject(command, classification)).orElse(result);
    } catch (final TimeoutException timeout) {
      cancelAndRemove(task);
      return reject(command, RuntimeFailureClassification.DEADLINE_EXCEEDED);
    } catch (final InterruptedException interrupted) {
      cancelAndRemove(task);
      Thread.currentThread().interrupt();
      return reject(command, RuntimeFailureClassification.INTERNAL_RUNTIME_FAILURE);
    } catch (final ExecutionException failure) {
      return reject(command, RuntimeFailureClassification.INTERNAL_RUNTIME_FAILURE);
    }
  }

  @Override
  public DecisionDryRunResult reject(
      final DecisionDryRunCommand command,
      final int status,
      final DecisionDryRunErrorCode errorCode,
      final String message) {
    return delegate.reject(command, status, errorCode, message);
  }

  /** @return 当前 active task 数，供 readiness evidence 使用。 */
  public int activeTaskCount() {
    return executor == null ? 0 : executor.getActiveCount();
  }

  /** @return 当前 bounded queue 中的 task 数。 */
  public int queuedTaskCount() {
    return executor == null ? 0 : executor.getQueue().size();
  }

  /** @return 配置的最大并发数。 */
  public int maxConcurrency() {
    return policy.concurrencyPolicy().maxConcurrency();
  }

  /** @return 配置的 bounded queue 容量。 */
  public int queueCapacity() {
    return policy.concurrencyPolicy().queueCapacity();
  }

  /** @return runtime executor 是否已经关闭。 */
  public boolean isClosed() {
    return closed.get();
  }

  /** 取消未完成任务并有界等待 worker 退出。 */
  @Override
  public void close() {
    if (!closed.compareAndSet(false, true) || executor == null) {
      return;
    }
    executor.shutdownNow();
    try {
      executor.awaitTermination(CLOSE_WAIT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (final InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private boolean awaitStart(final RuntimeFuture task, final Duration maxQueueWait) {
    if (policy.concurrencyPolicy().queueCapacity() == 0) {
      return true;
    }
    try {
      if (maxQueueWait.isZero()) {
        return task.hasStarted();
      }
      return task.awaitStarted(maxQueueWait.toNanos(), TimeUnit.NANOSECONDS);
    } catch (final InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private void cancelAndRemove(final RuntimeFuture task) {
    task.cancel(true);
    if (executor != null) {
      executor.remove(task);
    }
  }

  private DecisionDryRunResult reject(
      final DecisionDryRunCommand command,
      final RuntimeFailureClassification classification) {
    final int status;
    final DecisionDryRunErrorCode errorCode;
    switch (classification) {
      case DEADLINE_EXCEEDED -> {
        status = 504;
        errorCode = DecisionDryRunErrorCode.PROVIDER_TIMEOUT;
      }
      case CAPACITY_REJECTED -> {
        status = 503;
        errorCode = DecisionDryRunErrorCode.RATE_LIMITED;
      }
      case INTERNAL_RUNTIME_FAILURE -> {
        status = 500;
        errorCode = DecisionDryRunErrorCode.UNKNOWN_ERROR;
      }
      default -> {
        status = 403;
        errorCode = DecisionDryRunErrorCode.POLICY_DENIED;
      }
    }
    return delegate.reject(
        command,
        status,
        errorCode,
        "limited dry-run runtime rejected: " + classification.name());
  }

  private static ThreadPoolExecutor createExecutor(
      final LimitedDryRunRuntimePolicy policy) {
    final RuntimeConcurrencyPolicy concurrency = policy.concurrencyPolicy();
    if (!concurrency.isValid()
        || !policy.deadlinePolicy().isValid(concurrency.queueCapacity())) {
      return null;
    }
    final BlockingQueue<Runnable> queue =
        concurrency.queueCapacity() == 0
            ? new SynchronousQueue<>()
            : new ArrayBlockingQueue<>(concurrency.queueCapacity());
    final ThreadPoolExecutor bounded =
        new ThreadPoolExecutor(
            concurrency.maxConcurrency(),
            concurrency.maxConcurrency(),
            100L,
            TimeUnit.MILLISECONDS,
            queue,
            new RuntimeThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());
    bounded.allowCoreThreadTimeOut(true);
    return bounded;
  }

  private static final class RuntimeThreadFactory implements ThreadFactory {

    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public Thread newThread(final Runnable task) {
      final Thread thread =
          new Thread(task, "dh-limited-dryrun-" + sequence.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    }
  }

  private static final class RuntimeFuture extends FutureTask<DecisionDryRunResult> {

    private final CountDownLatch started = new CountDownLatch(1);

    private RuntimeFuture(final java.util.concurrent.Callable<DecisionDryRunResult> callable) {
      super(callable);
    }

    @Override
    public void run() {
      started.countDown();
      super.run();
    }

    private boolean hasStarted() {
      return started.getCount() == 0L;
    }

    private boolean awaitStarted(final long timeout, final TimeUnit unit)
        throws InterruptedException {
      return started.await(timeout, unit);
    }
  }
}
