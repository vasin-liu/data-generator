/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.audit.DatasourceAuditActions;
import org.gensokyo.data.audit.DatasourceAuditDetail;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.ConnectionHealthStatus;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.messaging.MessagingClusterConfigService;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.data.repository.MessagingClusterConfigRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Save-triggered hot-reload with DEGRADED fallback and last-known-good runtime retention (D-09, D-11).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@Service
public class HotReloadCoordinator {

    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final MessagingClusterConfigRepository messagingClusterConfigRepository;
    private final DataSourceConfigService dataSourceConfigService;
    private final MessagingClusterConfigService messagingClusterConfigService;
    private final AuditService auditService;

    private final ConcurrentHashMap<String, HealthOverlay> healthByKey = new ConcurrentHashMap<>();

    /**
     * @param dataSourceConfigRepository       JDBC config repository
     * @param messagingClusterConfigRepository messaging config repository
     * @param dataSourceConfigService          lazy JDBC registration service
     * @param messagingClusterConfigService    lazy messaging registration service
     * @param auditService                     audit trail for reload attempts
     */
    public HotReloadCoordinator(
            DataSourceConfigRepository dataSourceConfigRepository,
            MessagingClusterConfigRepository messagingClusterConfigRepository,
            @Lazy DataSourceConfigService dataSourceConfigService,
            @Lazy MessagingClusterConfigService messagingClusterConfigService,
            AuditService auditService) {
        this.dataSourceConfigRepository = dataSourceConfigRepository;
        this.messagingClusterConfigRepository = messagingClusterConfigRepository;
        this.dataSourceConfigService = dataSourceConfigService;
        this.messagingClusterConfigService = messagingClusterConfigService;
        this.auditService = auditService;
    }

    /**
     * Attempts to refresh runtime registries for a saved connection; on failure marks DEGRADED (D-11).
     *
     * @param name connection name
     * @param kind expected connection kind
     * @param baseEntry catalog list entry before health overlay merge
     * @return post-reload catalog entry including health metadata
     */
    public CatalogEntry reload(String name, ConnectionKind kind, CatalogEntry baseEntry) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        String trimmed = name.trim();
        String key = overlayKey(trimmed, kind);
        Instant now = Instant.now();
        long targetVersion = baseEntry != null ? baseEntry.version() : now.toEpochMilli();
        HealthOverlay previous = healthByKey.get(key);
        if (previous != null && previous.version() > targetVersion) {
            // Concurrent save won — avoid half-updated swap (D-08).
            targetVersion = previous.version();
        }
        try {
            switch (kind) {
                case JDBC -> reloadJdbc(trimmed);
                case KAFKA -> messagingClusterConfigService.reloadKafkaFromPersistence(trimmed);
                case ELASTICSEARCH -> messagingClusterConfigService.reloadElasticsearchFromPersistence(trimmed);
                default -> throw new IllegalArgumentException("Unsupported kind: " + kind);
            }
            HealthOverlay healthy = new HealthOverlay(
                    ConnectionHealthStatus.HEALTHY,
                    now,
                    null,
                    targetVersion);
            healthByKey.put(key, healthy);
            emitReloadAudit(trimmed, kind, "success", null);
            return overlay(baseEntry, healthy);
        } catch (Exception ex) {
            HealthOverlay degraded = new HealthOverlay(
                    ConnectionHealthStatus.DEGRADED,
                    now,
                    summarize(ex),
                    previous != null ? previous.version() : targetVersion);
            healthByKey.put(key, degraded);
            emitReloadAudit(trimmed, kind, "failure", summarize(ex));
            emitDegradedAudit(trimmed, kind, summarize(ex));
            return overlay(baseEntry, degraded);
        }
    }

    private void emitReloadAudit(String name, ConnectionKind kind, String outcome, String reason) {
        auditService.record(
                DatasourceAuditActions.RELOAD,
                DatasourceAuditActions.CATEGORY,
                name,
                DatasourceAuditDetail.summary(name, kind, DatasourceAuditActions.RELOAD, outcome, reason));
    }

    private void emitDegradedAudit(String name, ConnectionKind kind, String reason) {
        auditService.record(
                DatasourceAuditActions.DEGRADED,
                DatasourceAuditActions.CATEGORY,
                name,
                DatasourceAuditDetail.summary(name, kind, DatasourceAuditActions.DEGRADED, "degraded", reason));
    }

    /**
     * Returns health overlay for a catalog entry when present.
     *
     * @param name connection name
     * @param kind connection kind
     * @return optional health overlay
     */
    public Optional<HealthOverlay> healthOverlay(String name, ConnectionKind kind) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(healthByKey.get(overlayKey(name.trim(), kind)));
    }

    /**
     * Applies a health overlay onto a base catalog entry for list views.
     *
     * @param base    base entry from bootstrap/DB merge
     * @param overlay health overlay from reload attempts
     * @return merged catalog entry
     */
    public CatalogEntry overlay(CatalogEntry base, HealthOverlay overlay) {
        if (base == null) {
            return null;
        }
        if (overlay == null) {
            return base;
        }
        return new CatalogEntry(
                base.name(),
                base.kind(),
                base.source(),
                base.metadata(),
                overlay.version(),
                base.updatedAt(),
                overlay.healthStatus(),
                overlay.lastReloadAt(),
                overlay.degradedReason());
    }

    private void reloadJdbc(String name) {
        DataSourceConfigPO row = dataSourceConfigRepository.findById(name)
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown JDBC datasource: " + name));
        dataSourceConfigService.registerToRuntime(row);
    }

    private static String overlayKey(String name, ConnectionKind kind) {
        return kind.name() + ":" + name;
    }

    private static String summarize(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 512 ? message.substring(0, 512) : message;
    }

    /**
     * Mutable health state for a catalog entry after reload attempts.
     *
     * @param healthStatus   HEALTHY or DEGRADED
     * @param lastReloadAt   timestamp of the most recent reload attempt
     * @param degradedReason operator-facing failure summary when DEGRADED
     * @param version        pinned generation counter
     */
    public record HealthOverlay(
            ConnectionHealthStatus healthStatus,
            Instant lastReloadAt,
            String degradedReason,
            long version) {
    }
}
