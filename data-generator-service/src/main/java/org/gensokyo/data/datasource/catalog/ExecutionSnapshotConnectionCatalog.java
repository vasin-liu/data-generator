/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.calcite.runtime.WorkflowRunContext;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogResolveSupport;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ConnectionTestRequest;
import org.gensokyo.data.datasource.api.ConnectionTestResult;
import org.gensokyo.data.datasource.api.ResolvedConnection;
import org.gensokyo.data.datasource.api.snapshot.ExecutionConnectionSnapshot;
import org.gensokyo.data.datasource.api.snapshot.SnapshottedConnectionRef;
import org.gensokyo.data.task.TaskExecutionService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorator resolving from per-execution snapshots for in-flight runs; delegates to live catalog otherwise (D-07, D-10).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@Service
@Primary
@RequiredArgsConstructor
public class ExecutionSnapshotConnectionCatalog implements ConnectionCatalog {

    private final ConnectionCatalogImpl delegate;
    private final TaskExecutionService taskExecutionService;
    private final SnapshotConnectionMaterializer materializer;

    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, ResolvedConnection>> materializedByInstance =
            new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public ResolvedConnection resolve(String name, ConnectionKind kind) {
        Long instanceId = WorkflowRunContext.instanceId();
        if (instanceId == null) {
            return delegate.resolve(name, kind);
        }
        return resolveFromSnapshot(instanceId, name, kind);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CatalogEntry> listAll() {
        return delegate.listAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionTestResult test(ConnectionTestRequest request) {
        return delegate.test(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CatalogEntry reload(String name, ConnectionKind kind) {
        return delegate.reload(name, kind);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CatalogEntry> findEntry(String name, ConnectionKind kind) {
        return delegate.findEntry(name, kind);
    }

    /**
     * Evicts per-instance materialized handles when an execution reaches a terminal state.
     *
     * @param instanceId finished run instance id
     */
    public void evictInstance(Long instanceId) {
        if (instanceId != null) {
            materializedByInstance.remove(instanceId);
        }
    }

    private ResolvedConnection resolveFromSnapshot(Long instanceId, String name, ConnectionKind kind) {
        if (name == null || name.isBlank()) {
            throw CatalogResolveSupport.unknownConnection(name, kind, "Connection name must not be blank");
        }
        ExecutionConnectionSnapshot snapshot = taskExecutionService.getConnectionSnapshot(instanceId)
                .orElseThrow(() -> CatalogResolveSupport.unknownConnection(
                        name,
                        kind,
                        "No connection snapshot for active execution " + instanceId));
        SnapshottedConnectionRef ref = findRef(snapshot, name.trim(), kind)
                .orElseThrow(() -> CatalogResolveSupport.unknownConnection(
                        name,
                        kind,
                        "Connection not present in execution snapshot"));
        String cacheKey = kind.name() + ":" + ref.name();
        ConcurrentHashMap<String, ResolvedConnection> instanceCache =
                materializedByInstance.computeIfAbsent(instanceId, ignored -> new ConcurrentHashMap<>());
        return instanceCache.computeIfAbsent(cacheKey, ignored -> materializer.materialize(instanceId, ref));
    }

    private static Optional<SnapshottedConnectionRef> findRef(
            ExecutionConnectionSnapshot snapshot,
            String name,
            ConnectionKind kind) {
        Objects.requireNonNull(snapshot, "snapshot");
        return snapshot.connections().stream()
                .filter(ref -> ref.kind() == kind && ref.name().equals(name))
                .findFirst();
    }
}
