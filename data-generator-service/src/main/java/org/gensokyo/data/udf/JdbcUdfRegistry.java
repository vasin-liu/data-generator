/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.model.po.UdfArtifactPO;
import org.gensokyo.data.repository.UdfArtifactRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * JDBC-backed {@link UdfRegistry} that persists artifact rows through {@link UdfArtifactRepository} (D-01).
 *
 * <p>This implementation preserves the exact Phase 2 contract from {@code InMemoryUdfRegistry}: the same
 * reverse-DNS {@code udfId} and strict semver validation, the same {@link UdfRegistryException} codes, the
 * same {@code DRAFT → PUBLISHED → DEPRECATED} finite-state machine, and the same latest-published semver
 * resolution. Only the storage changes — the {@code ConcurrentHashMap} is replaced by repository CRUD, so
 * registered/published UDFs survive restart (D-01) and can be rehydrated into the runtime on startup (D-02).
 * Published rows are immutable; transitions only change state and stamp timestamps (D-08). The registry stays
 * global (D-03).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
public final class JdbcUdfRegistry implements UdfRegistry {

    private static final Pattern UDF_ID_PATTERN =
            Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {
    };

    private final UdfArtifactRepository repository;

    /**
     * @param repository backing artifact repository
     */
    public JdbcUdfRegistry(UdfArtifactRepository repository) {
        this.repository = repository;
    }

    @Override
    public UdfRecord registerDraft(String udfId, String version, UdfType type, byte[] payload,
                                   Map<String, String> metadata) {
        String normalizedId = validateUdfId(udfId);
        String normalizedVersion = validateVersion(version);
        if (type == null) {
            throw new UdfRegistryException("UDF_INVALID_TYPE", "UDF type is required");
        }
        // Duplicate udfId+version is rejected; older versions are retained as history (D-08).
        if (repository.findByUdfIdAndVersion(normalizedId, normalizedVersion).isPresent()) {
            throw new UdfRegistryException("UDF_DUPLICATE_VERSION",
                    "UDF already registered: " + normalizedId + "@" + normalizedVersion);
        }
        UdfArtifactPO entity = new UdfArtifactPO();
        entity.setUdfId(normalizedId);
        entity.setVersion(normalizedVersion);
        entity.setType(type.jsonName());
        entity.setState(UdfLifecycleState.DRAFT.name());
        entity.setPayload(payload == null ? new byte[0] : payload.clone());
        entity.setMetadataJson(serializeMetadata(metadata));
        entity.setRegisteredAt(Instant.now());
        return toRecord(repository.save(entity));
    }

    @Override
    public UdfRecord publish(String udfId, String version) {
        UdfArtifactPO entity = requireExisting(udfId, version);
        UdfLifecycleState current = UdfLifecycleState.valueOf(entity.getState());
        if (current == UdfLifecycleState.PUBLISHED) {
            return toRecord(entity);
        }
        if (current != UdfLifecycleState.DRAFT) {
            throw new UdfRegistryException("UDF_INVALID_TRANSITION",
                    "Cannot publish UDF in state " + current);
        }
        // Published rows are immutable beyond the state/timestamp transition (D-08).
        entity.setState(UdfLifecycleState.PUBLISHED.name());
        entity.setPublishedAt(Instant.now());
        return toRecord(repository.save(entity));
    }

    @Override
    public UdfRecord deprecate(String udfId, String version) {
        UdfArtifactPO entity = requireExisting(udfId, version);
        UdfLifecycleState current = UdfLifecycleState.valueOf(entity.getState());
        if (current == UdfLifecycleState.DEPRECATED) {
            return toRecord(entity);
        }
        if (current != UdfLifecycleState.PUBLISHED) {
            throw new UdfRegistryException("UDF_INVALID_TRANSITION",
                    "Cannot deprecate UDF in state " + current);
        }
        entity.setState(UdfLifecycleState.DEPRECATED.name());
        entity.setDeprecatedAt(Instant.now());
        return toRecord(repository.save(entity));
    }

    @Override
    public List<UdfRecord> list(Optional<UdfType> typeFilter) {
        List<UdfRecord> result = new ArrayList<>();
        for (UdfArtifactPO entity : repository.findAll()) {
            UdfRecord record = toRecord(entity);
            if (typeFilter.isEmpty() || typeFilter.get() == record.type()) {
                result.add(record);
            }
        }
        result.sort(Comparator.comparing(UdfRecord::udfId)
                .thenComparing(UdfRecord::version, JdbcUdfRegistry::compareSemver));
        return List.copyOf(result);
    }

    @Override
    public Optional<UdfRecord> find(String udfId, String version) {
        return repository.findByUdfIdAndVersion(validateUdfId(udfId), validateVersion(version))
                .map(this::toRecord);
    }

    @Override
    public UdfRecord resolve(String udfId, Optional<String> versionOptional) {
        String normalizedId = validateUdfId(udfId);
        if (versionOptional.isPresent() && !versionOptional.get().isBlank()) {
            UdfRecord exact = toRecord(requireExisting(normalizedId, versionOptional.get()));
            return requirePublished(exact);
        }
        // Latest published semver for this udfId (D-17).
        return repository.findByUdfIdOrderByVersionAsc(normalizedId).stream()
                .map(this::toRecord)
                .filter(r -> r.state() == UdfLifecycleState.PUBLISHED)
                .max(Comparator.comparing(UdfRecord::version, JdbcUdfRegistry::compareSemver))
                .orElseThrow(() -> new UdfRegistryException("UDF_NOT_FOUND",
                        "No published UDF found for id " + normalizedId));
    }

    private UdfArtifactPO requireExisting(String udfId, String version) {
        String normalizedId = validateUdfId(udfId);
        String normalizedVersion = validateVersion(version);
        return repository.findByUdfIdAndVersion(normalizedId, normalizedVersion)
                .orElseThrow(() -> new UdfRegistryException("UDF_NOT_FOUND",
                        "UDF not found: " + normalizedId + "@" + normalizedVersion));
    }

    private static UdfRecord requirePublished(UdfRecord record) {
        if (record.state() == UdfLifecycleState.DRAFT) {
            throw new UdfRegistryException("UDF_NOT_PUBLISHED",
                    "UDF is not published: " + record.udfId() + "@" + record.version());
        }
        if (record.state() == UdfLifecycleState.DEPRECATED) {
            throw new UdfRegistryException("UDF_DEPRECATED",
                    "UDF is deprecated: " + record.udfId() + "@" + record.version());
        }
        return record;
    }

    private UdfRecord toRecord(UdfArtifactPO entity) {
        return new UdfRecord.Builder()
                .udfId(entity.getUdfId())
                .version(entity.getVersion())
                .type(UdfType.fromValue(entity.getType()))
                .state(UdfLifecycleState.valueOf(entity.getState()))
                .payload(entity.getPayload() == null ? new byte[0] : entity.getPayload())
                .metadata(deserializeMetadata(entity.getMetadataJson()))
                .registeredAt(entity.getRegisteredAt())
                .publishedAt(entity.getPublishedAt())
                .deprecatedAt(entity.getDeprecatedAt())
                .build();
    }

    private static String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        return MAPPER.writeValueAsString(metadata);
    }

    private static Map<String, String> deserializeMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return MAPPER.readValue(json, METADATA_TYPE);
    }

    private static String validateUdfId(String udfId) {
        if (udfId == null || udfId.isBlank()) {
            throw new UdfRegistryException("UDF_INVALID_ID", "udfId is required");
        }
        String normalized = udfId.trim().toLowerCase(Locale.ROOT);
        if (!UDF_ID_PATTERN.matcher(normalized).matches()) {
            throw new UdfRegistryException("UDF_INVALID_ID",
                    "udfId must be reverse-DNS (e.g. com.example.my_udf): " + udfId);
        }
        return normalized;
    }

    private static String validateVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new UdfRegistryException("UDF_INVALID_VERSION", "version is required");
        }
        String normalized = version.trim();
        if (!SEMVER_PATTERN.matcher(normalized).matches()) {
            throw new UdfRegistryException("UDF_INVALID_VERSION",
                    "version must be semver major.minor.patch: " + version);
        }
        return normalized;
    }

    private static int compareSemver(String left, String right) {
        int[] l = parseSemver(left);
        int[] r = parseSemver(right);
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(l[i], r[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static int[] parseSemver(String version) {
        String[] parts = version.split("\\.");
        return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
