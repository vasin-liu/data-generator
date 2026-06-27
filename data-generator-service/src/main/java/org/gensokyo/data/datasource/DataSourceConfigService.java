/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.catalog.ConnectionCatalogImpl;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.data.secret.SecretResolver;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Persists JDBC datasource definitions and syncs them to {@link DynamicRoutingDataSource}.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class DataSourceConfigService {

    private final DataSourceConfigRepository repository;
    private final ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider;
    private final DataSourceDriverSupport driverSupport;
    private final SecretResolver secretResolver;
    private final AuditService auditService;
    private final ConnectionCatalogImpl connectionCatalog;

    /**
     * @return summaries of all persisted configs
     */
    public List<DataSourceConfigSummary> listAll() {
        return repository.findAll().stream().map(this::toSummary).toList();
    }

    /**
     * @return runtime datasource keys (yaml + persisted)
     */
    public Set<String> listRuntimeNames() {
        DynamicRoutingDataSource routing = requireRouting();
        return routing.getDataSources().keySet();
    }

    /**
     * Adds or updates a datasource in DB and runtime.
     *
     * @param name            unique name
     * @param url             JDBC URL
     * @param username        user
     * @param password        password
     * @param driverClassName driver class
     * @param driverFile      optional driver jar upload
     * @return summary
     */
    @Transactional
    public DataSourceConfigSummary save(
            String name,
            String url,
            String username,
            String password,
            String passwordSecretRef,
            String driverClassName,
            MultipartFile driverFile) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Datasource name must be set");
        }
        Instant now = Instant.now();
        DataSourceConfigPO entity = repository.findById(name).orElseGet(DataSourceConfigPO::new);
        boolean isNew = entity.getName() == null;
        entity.setName(name);
        entity.setUrl(url);
        entity.setUsername(username);
        if (StrKit.isNotBlank(passwordSecretRef)) {
            entity.setPasswordSecretRef(passwordSecretRef.trim());
            entity.setPassword(null);
        } else {
            entity.setPasswordSecretRef(null);
            // Keep stored password when UI leaves the field blank on edit.
            if (password != null && !password.isBlank()) {
                entity.setPassword(password);
            } else if (isNew) {
                entity.setPassword(password);
            }
        }
        entity.setDriverClassName(driverClassName);
        entity.setEnabled(Boolean.TRUE);
        if (isNew) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        if (driverFile != null && !driverFile.isEmpty()) {
            try {
                entity.setDriverJarPath(driverSupport.storeDriverJar(driverFile));
            } catch (Exception e) {
                throw new DataGeneratorException("Failed to store driver JAR", e);
            }
        }
        DataSourceConfigPO saved = repository.saveAndFlush(entity);
        connectionCatalog.reload(saved.getName(), ConnectionKind.JDBC);
        auditService.record(
                isNew ? "DATASOURCE_CREATE" : "DATASOURCE_UPDATE",
                "DATASOURCE",
                saved.getName(),
                java.util.Map.of("url", saved.getUrl()));
        return toSummary(saved);
    }

    /**
     * Removes a datasource from runtime and disables persisted row when present.
     *
     * @param name datasource key
     */
    @Transactional
    public void remove(String name) {
        if (connectionCatalog.isBootstrapOnly(name)) {
            throw new IllegalArgumentException("Bootstrap datasource cannot be removed via console: " + name);
        }
        DynamicRoutingDataSource routing = requireRouting();
        if (routing.getDataSources().containsKey(name)) {
            routing.removeDataSource(name);
        }
        repository.findById(name).ifPresent(row -> {
            row.setEnabled(Boolean.FALSE);
            row.setUpdatedAt(Instant.now());
            repository.saveAndFlush(row);
        });
    }

    /**
     * Tests JDBC connectivity without persisting.
     *
     * @param url             JDBC URL
     * @param username        user
     * @param password        password
     * @param driverClassName driver class
     * @param driverJarPath   optional jar path
     * @return success message
     */
    public String testConnection(
            String url,
            String username,
            String password,
            String driverClassName,
            String driverJarPath) {
        try {
            JdbcDriverLoadResult loaded = driverSupport.ensureDriverLoaded(driverClassName, url, driverJarPath);
            try (Connection connection = driverSupport.openConnection(url, username, password, loaded)) {
                if (!connection.isValid(5)) {
                    throw new IllegalStateException("Connection invalid");
                }
            }
            String suffix = buildLoadMessageSuffix(loaded, driverClassName);
            return "Connection OK" + suffix;
        } catch (DataGeneratorException e) {
            throw e;
        } catch (Exception e) {
            throw new DataGeneratorException("Connection test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Tests a persisted datasource by name.
     *
     * @param name datasource key
     * @return success message
     */
    public String testConnectionByName(String name) {
        DataSourceConfigPO row = repository.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown datasource: " + name));
        return testConnection(
                row.getUrl(),
                row.getUsername(),
                resolvePassword(row),
                row.getDriverClassName(),
                row.getDriverJarPath());
    }

    /**
     * Registers all enabled rows into the dynamic routing datasource.
     */
    public void bootstrapEnabled() {
        for (DataSourceConfigPO row : repository.findByEnabledTrue()) {
            try {
                registerToRuntime(row);
            } catch (Exception e) {
                throw new DataGeneratorException("Failed to bootstrap datasource " + row.getName(), e);
            }
        }
    }

    /**
     * Adds or replaces one row in the runtime registry.
     *
     * @param row persisted config
     */
    public void registerToRuntime(DataSourceConfigPO row) {
        DynamicRoutingDataSource routing = requireRouting();
        try {
            JdbcDriverLoadResult loaded = driverSupport.ensureDriverLoaded(
                    row.getDriverClassName(), row.getUrl(), row.getDriverJarPath());
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(row.getUrl());
            dataSource.setUsername(row.getUsername());
            dataSource.setPassword(resolvePassword(row));
            dataSource.setDriverClassName(loaded.driverClassName());
            dataSource.setDriverClassLoader(loaded.classLoader());
            if (routing.getDataSources().containsKey(row.getName())) {
                routing.removeDataSource(row.getName());
            }
            routing.addDataSource(row.getName(), dataSource);
        } catch (Exception e) {
            throw new DataGeneratorException("Failed to register datasource " + row.getName(), e);
        }
    }

    private DynamicRoutingDataSource requireRouting() {
        DynamicRoutingDataSource routing = dynamicRoutingDataSourceProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("DynamicRoutingDataSource is not available");
        }
        return routing;
    }

    private String resolvePassword(DataSourceConfigPO row) {
        return secretResolver.resolveInlinePassword(row.getPassword(), row.getPasswordSecretRef());
    }

    private DataSourceConfigSummary toSummary(DataSourceConfigPO row) {
        return new DataSourceConfigSummary(
                row.getName(),
                row.getUrl(),
                row.getUsername(),
                row.getDriverClassName(),
                row.getDriverJarPath(),
                row.getPasswordSecretRef(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private static String buildLoadMessageSuffix(JdbcDriverLoadResult loaded, String requestedClass) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (!loaded.driverClassName().equals(requestedClass)) {
            parts.add("driver loaded as " + loaded.driverClassName());
        }
        if (loaded.bundled()) {
            parts.add("bundled driver");
        }
        if (parts.isEmpty()) {
            return "";
        }
        return " (" + String.join(", ", parts) + ")";
    }
}
