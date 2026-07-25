package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceTransactionBoundary;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL feedback aggregate 的 required/repeatable-read 单事务边界。 */
public final class JdbcFeedbackPersistenceTransactionBoundary
    implements FeedbackPersistenceTransactionBoundary {

  private final TransactionTemplate transactionTemplate;

  /**
   * 创建强制事务边界。
   *
   * <p>manager 缺失时构造即失败；commit phase 无法证明时返回专用 fail-closed 分类，绝不自动重试。
   */
  public JdbcFeedbackPersistenceTransactionBoundary(
      final PlatformTransactionManager transactionManager) {
    final PlatformTransactionManager checked =
        Objects.requireNonNull(transactionManager, "transactionManager");
    this.transactionTemplate = new TransactionTemplate(checked);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  @Override
  public <T> T requiredRepeatableRead(final Supplier<T> action) {
    final Supplier<T> checked = Objects.requireNonNull(action, "action");
    try {
      final T result =
          transactionTemplate.execute(
              status -> Objects.requireNonNull(checked.get(), "transaction result"));
      return Objects.requireNonNull(result, "transaction result");
    } catch (final FeedbackPersistenceException error) {
      throw error;
    } catch (final TransactionSystemException error) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.COMMIT_OUTCOME_UNKNOWN,
          "feedback aggregate commit outcome cannot be proven",
          error);
    } catch (final CannotCreateTransactionException error) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.PERSISTENCE_FAILURE,
          "feedback aggregate transaction cannot be created",
          error);
    } catch (final TransactionException error) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.PERSISTENCE_FAILURE,
          "feedback aggregate transaction failed",
          error);
    }
  }
}
