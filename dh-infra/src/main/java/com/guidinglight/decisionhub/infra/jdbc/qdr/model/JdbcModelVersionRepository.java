package com.guidinglight.decisionhub.infra.jdbc.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionChecksumConflictException;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;
import com.guidinglight.decisionhub.usecase.qdr.model.SaveModelVersionCommand;

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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ModelVersion JDBC persistence adapter。
 *
 * <p>本 adapter 只访问 DH 自身 `qdr_model_profile` / `qdr_model_version` 表。所有查询都带 `tenant_id`，
 * 不提供 UUID-only 访问；不同 checksum 的重复 model version 必须 fail-closed。不调用 HTTP、provider、
 * NQ 或交易路径。
 */
public final class JdbcModelVersionRepository implements ModelVersionPersistencePort {

    private static final String COLUMNS =
            "mv.model_profile_id, mv.id as model_version_id, mp.provider_profile_id,"
                    + " mv.tenant_id, mp.provider_kind, mp.provider_key, mp.model_key,"
                    + " mp.display_name, mv.capability_summary, mp.context_window_tokens,"
                    + " mp.max_output_tokens, mp.profile_status, mp.trust_policy_ref,"
                    + " mv.model_name, mv.model_version, mv.checksum, mv.created_at";

    private static final String INSERT_PROFILE =
            "insert into qdr_model_profile"
                    + " (id, tenant_id, provider_profile_id, provider_kind, provider_key, model_key,"
                    + " display_name, capability_summary, context_window_tokens, max_output_tokens,"
                    + " profile_status, trust_policy_ref, created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " on conflict (tenant_id, model_key) do nothing";

    private static final String INSERT_VERSION =
            "insert into qdr_model_version"
                    + " (id, tenant_id, model_profile_id, model_name, model_version,"
                    + " capability_summary, version_status, checksum, created_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_TENANT_MODEL_VERSION_ID =
            "select " + COLUMNS
                    + " from qdr_model_version mv"
                    + " join qdr_model_profile mp on mp.id = mv.model_profile_id"
                    + " where mv.tenant_id = ? and mv.id = ?"
                    + " limit 1";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JDBC adapter。
     *
     * @param jdbcTemplate DH datasource 对应的 JDBC template。
     */
    public JdbcModelVersionRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public ModelVersionRecord save(final SaveModelVersionCommand command) {
        final SaveModelVersionCommand checked = Objects.requireNonNull(command, "command");
        final Optional<ModelVersionRecord> existing =
                findByTenantAndModelVersionId(checked.tenantId(), checked.modelVersionId());
        if (existing.isPresent()) {
            return existingOrConflict(existing.get(), checked.checksum());
        }

        try {
            jdbcTemplate.update(
                    INSERT_PROFILE,
                    checked.modelProfileId(),
                    checked.tenantId(),
                    checked.providerProfileId(),
                    checked.providerKind().name(),
                    checked.providerKey(),
                    checked.modelKey(),
                    checked.displayName(),
                    checked.capabilitySummary(),
                    checked.contextWindowTokens(),
                    checked.maxOutputTokens(),
                    checked.profileStatus().name(),
                    checked.trustPolicyRef(),
                    timestamp(checked.createdAt()),
                    timestamp(checked.createdAt()));
            jdbcTemplate.update(
                    INSERT_VERSION,
                    checked.modelVersionId(),
                    checked.tenantId(),
                    checked.modelProfileId(),
                    checked.modelName(),
                    checked.modelVersion(),
                    checked.capabilitySummary(),
                    "ACTIVE",
                    checked.checksum(),
                    timestamp(checked.createdAt()));
            return toRecord(checked);
        } catch (final DuplicateKeyException error) {
            return findByTenantAndModelVersionId(checked.tenantId(), checked.modelVersionId())
                    .map(found -> existingOrConflict(found, checked.checksum()))
                    .orElseThrow(ModelVersionChecksumConflictException::new);
        } catch (final DataAccessException error) {
            throw new ModelVersionPersistenceException("save model version failed", error);
        }
    }

    @Override
    public Optional<ModelVersionRecord> findByTenantAndModelVersionId(
            final String tenantId, final UUID modelVersionId) {
        try {
            return jdbcTemplate
                    .queryForList(
                            SELECT_BY_TENANT_MODEL_VERSION_ID,
                            QdrPersistenceSafety.requireText(tenantId, "tenantId"),
                            QdrPersistenceSafety.requireUuid(modelVersionId, "modelVersionId"))
                    .stream()
                    .findFirst()
                    .map(this::mapRecord);
        } catch (final DataAccessException error) {
            throw new ModelVersionPersistenceException("find model version failed", error);
        } catch (final RuntimeException error) {
            if (error instanceof ModelVersionPersistenceException persistenceException) {
                throw persistenceException;
            }
            throw new ModelVersionPersistenceException("find model version rejected", error);
        }
    }

    private static ModelVersionRecord existingOrConflict(
            final ModelVersionRecord existing, final String checksum) {
        final String checkedChecksum = QdrPersistenceSafety.requireSha256Hex(checksum, "checksum");
        if (!existing.checksum().equals(checkedChecksum)) {
            throw new ModelVersionChecksumConflictException();
        }
        return existing;
    }

    private static ModelVersionRecord toRecord(final SaveModelVersionCommand command) {
        return new ModelVersionRecord(
                command.modelProfileId(),
                command.modelVersionId(),
                command.providerProfileId(),
                command.tenantId(),
                command.providerKind(),
                command.providerKey(),
                command.modelKey(),
                command.displayName(),
                command.capabilitySummary(),
                command.contextWindowTokens(),
                command.maxOutputTokens(),
                command.profileStatus(),
                command.trustPolicyRef(),
                command.modelName(),
                command.modelVersion(),
                command.checksum(),
                command.createdAt());
    }

    private ModelVersionRecord mapRecord(final Map<String, Object> row) {
        return new ModelVersionRecord(
                uuid(row, "model_profile_id"),
                uuid(row, "model_version_id"),
                uuid(row, "provider_profile_id"),
                text(row, "tenant_id"),
                enumValue(row, "provider_kind", ProviderKind.class),
                text(row, "provider_key"),
                text(row, "model_key"),
                text(row, "display_name"),
                text(row, "capability_summary"),
                integer(row, "context_window_tokens"),
                integer(row, "max_output_tokens"),
                enumValue(row, "profile_status", ProviderProfileStatus.class),
                optionalText(row, "trust_policy_ref"),
                text(row, "model_name"),
                text(row, "model_version"),
                text(row, "checksum"),
                instant(row, "created_at"));
    }

    private static Timestamp timestamp(final Instant value) {
        return Timestamp.from(Objects.requireNonNull(value, "value"));
    }

    private static <E extends Enum<E>> E enumValue(
            final Map<String, Object> row, final String key, final Class<E> enumType) {
        return Enum.valueOf(enumType, text(row, key));
    }

    private static String text(final Map<String, Object> row, final String key) {
        final String value = optionalText(row, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must not be null");
        }
        return value;
    }

    private static String optionalText(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        final String checked = value.toString().trim();
        return checked.isEmpty() ? null : checked;
    }

    private static UUID uuid(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static int integer(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
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
