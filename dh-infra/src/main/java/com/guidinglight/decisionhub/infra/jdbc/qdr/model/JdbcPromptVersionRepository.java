package com.guidinglight.decisionhub.infra.jdbc.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionChecksumConflictException;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;
import com.guidinglight.decisionhub.usecase.qdr.model.SavePromptVersionCommand;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PromptVersion JDBC persistence adapter。
 *
 * <p>本 adapter 只访问 DH 自身 `qdr_prompt_template` / `qdr_prompt_version` 表。所有查询都带 `tenant_id`，不提供
 * UUID-only 访问；保存不同 checksum 的重复版本必须 fail-closed。不调用 HTTP、 provider、NQ 或交易路径。
 */
public final class JdbcPromptVersionRepository implements PromptVersionPersistencePort {

  private static final String COLUMNS =
      "pv.prompt_template_id, pv.id as prompt_version_id, pv.tenant_id, pt.template_key,"
          + " pv.version, pv.render_policy_key, pv.template_ref, pv.template_hash,"
          + " pv.redacted_summary, pv.status, pv.checksum, pv.created_at, pv.created_by";

  private static final String INSERT_TEMPLATE =
      "insert into qdr_prompt_template"
          + " (id, tenant_id, template_key, display_name, current_version_id, status,"
          + " created_at, updated_at)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?)"
          + " on conflict do nothing";

  private static final String INSERT_VERSION =
      "insert into qdr_prompt_version"
          + " (id, tenant_id, prompt_template_id, version, render_policy_key, template_ref,"
          + " template_hash, redacted_summary, status, checksum, created_at, created_by)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
          + " on conflict do nothing";

  private static final String SELECT_BY_TENANT_TEMPLATE_VERSION =
      "select "
          + COLUMNS
          + " from qdr_prompt_version pv"
          + " join qdr_prompt_template pt"
          + " on pt.id = pv.prompt_template_id and pt.tenant_id = pv.tenant_id"
          + " where pv.tenant_id = ? and pv.prompt_template_id = ? and pv.version = ?"
          + " limit 1";

  private static final String SELECT_BY_TENANT_VERSION_ID =
      "select "
          + COLUMNS
          + " from qdr_prompt_version pv"
          + " join qdr_prompt_template pt"
          + " on pt.id = pv.prompt_template_id and pt.tenant_id = pv.tenant_id"
          + " where pv.tenant_id = ? and pv.id = ?";

  private final JdbcTemplate jdbcTemplate;

  /**
   * 创建 JDBC adapter。
   *
   * @param jdbcTemplate DH datasource 对应的 JDBC template。
   */
  public JdbcPromptVersionRepository(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public PromptVersionRecord save(final SavePromptVersionCommand command) {
    final SavePromptVersionCommand checked = Objects.requireNonNull(command, "command");
    try {
      jdbcTemplate.update(
          INSERT_TEMPLATE,
          checked.promptTemplateId(),
          checked.tenantId(),
          checked.templateKey(),
          checked.templateDisplayName(),
          checked.promptVersionId(),
          checked.status().name(),
          timestamp(checked.createdAt()),
          timestamp(checked.createdAt()));
      jdbcTemplate.update(
          INSERT_VERSION,
          checked.promptVersionId(),
          checked.tenantId(),
          checked.promptTemplateId(),
          checked.version(),
          checked.renderPolicyKey(),
          checked.templateRef(),
          checked.templateHash(),
          checked.redactedSummary(),
          checked.status().name(),
          checked.checksum(),
          timestamp(checked.createdAt()),
          checked.createdBy());
      return findByTenantAndTemplateVersion(
              checked.tenantId(), checked.promptTemplateId(), checked.version())
          .map(found -> existingOrConflict(found, checked))
          .orElseThrow(PromptVersionChecksumConflictException::new);
    } catch (final DataAccessException error) {
      throw new PromptVersionPersistenceException("save prompt version failed", error);
    }
  }

  @Override
  public Optional<PromptVersionRecord> findByTenantAndTemplateVersion(
      final String tenantId, final UUID promptTemplateId, final String version) {
    try {
      return jdbcTemplate
          .queryForList(
              SELECT_BY_TENANT_TEMPLATE_VERSION,
              QdrPersistenceSafety.requireText(tenantId, "tenantId"),
              QdrPersistenceSafety.requireUuid(promptTemplateId, "promptTemplateId"),
              QdrPersistenceSafety.requireSafeText(version, "version"))
          .stream()
          .findFirst()
          .map(this::mapRecord);
    } catch (final DataAccessException error) {
      throw new PromptVersionPersistenceException("find prompt version failed", error);
    } catch (final RuntimeException error) {
      if (error instanceof PromptVersionPersistenceException persistenceException) {
        throw persistenceException;
      }
      throw new PromptVersionPersistenceException("find prompt version rejected", error);
    }
  }

  @Override
  public Optional<PromptVersionRecord> findByTenantAndPromptVersionId(
      final String tenantId, final UUID promptVersionId) {
    final String checkedTenant = QdrPersistenceSafety.requireText(tenantId, "tenantId");
    final UUID checkedId = QdrPersistenceSafety.requireUuid(promptVersionId, "promptVersionId");
    try {
      final var rows =
          jdbcTemplate.queryForList(SELECT_BY_TENANT_VERSION_ID, checkedTenant, checkedId);
      if (rows.size() > 1) {
        throw new PromptVersionPersistenceException(
            "prompt version exact identity returned multiple rows");
      }
      return rows.stream()
          .findFirst()
          .map(this::mapRecord)
          .map(
              record -> {
                if (!checkedTenant.equals(record.tenantId())
                    || !checkedId.equals(record.promptVersionId())) {
                  throw new PromptVersionPersistenceException(
                      "prompt version exact identity mismatch");
                }
                return record;
              });
    } catch (final DataAccessException error) {
      throw new PromptVersionPersistenceException("find prompt version by id failed", error);
    } catch (final RuntimeException error) {
      if (error instanceof PromptVersionPersistenceException persistenceException) {
        throw persistenceException;
      }
      throw new PromptVersionPersistenceException("find prompt version by id rejected", error);
    }
  }

  /**
   * 比较 immutable PromptVersion 的稳定身份与行为语义。
   *
   * <p>{@code createdAt}/{@code createdBy} 是 canonical row 的审计元数据，不参与幂等竞争；竞争输家必须返回 已提交行的 canonical
   * metadata。其余字段会影响 tenant identity、prompt 内容、render policy 或安全状态， 任一不一致都必须 fail-closed，且绝不覆盖既有行。
   */
  private static PromptVersionRecord existingOrConflict(
      final PromptVersionRecord existing, final SavePromptVersionCommand command) {
    if (!existing.promptTemplateId().equals(command.promptTemplateId())
        || !existing.promptVersionId().equals(command.promptVersionId())
        || !existing.tenantId().equals(command.tenantId())
        || !existing.templateKey().equals(command.templateKey())
        || !existing.version().equals(command.version())
        || !existing.renderPolicyKey().equals(command.renderPolicyKey())
        || !existing.templateRef().equals(command.templateRef())
        || !existing.templateHash().equals(command.templateHash())
        || !existing.redactedSummary().equals(command.redactedSummary())
        || existing.status() != command.status()
        || !existing.checksum().equals(command.checksum())) {
      throw new PromptVersionChecksumConflictException();
    }
    return existing;
  }

  private PromptVersionRecord mapRecord(final Map<String, Object> row) {
    return new PromptVersionRecord(
        uuid(row, "prompt_template_id"),
        uuid(row, "prompt_version_id"),
        text(row, "tenant_id"),
        text(row, "template_key"),
        text(row, "version"),
        text(row, "render_policy_key"),
        text(row, "template_ref"),
        text(row, "template_hash"),
        text(row, "redacted_summary"),
        enumValue(row, "status", PromptVersionStatus.class),
        text(row, "checksum"),
        instant(row, "created_at"),
        text(row, "created_by"));
  }

  private static Timestamp timestamp(final Instant value) {
    return Timestamp.from(Objects.requireNonNull(value, "value"));
  }

  private static <E extends Enum<E>> E enumValue(
      final Map<String, Object> row, final String key, final Class<E> enumType) {
    return Enum.valueOf(enumType, text(row, key));
  }

  private static String text(final Map<String, Object> row, final String key) {
    final Object value = Objects.requireNonNull(row.get(key), key);
    final String checked = value.toString().trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(key + " must not be blank");
    }
    return checked;
  }

  private static UUID uuid(final Map<String, Object> row, final String key) {
    final Object value = Objects.requireNonNull(row.get(key), key);
    if (value instanceof UUID uuid) {
      return uuid;
    }
    return UUID.fromString(value.toString());
  }

  private static Instant instant(final Map<String, Object> row, final String key) {
    final Object value = Objects.requireNonNull(row.get(key), key);
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof OffsetDateTime dateTime) {
      return dateTime.toInstant();
    }
    if (value instanceof LocalDateTime dateTime) {
      return dateTime.toInstant(ZoneOffset.UTC);
    }
    return Instant.parse(value.toString());
  }
}
