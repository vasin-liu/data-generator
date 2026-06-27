/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionHealthStatus;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ConnectionTestRequest;
import org.gensokyo.data.datasource.api.ConnectionTestResult;
import org.gensokyo.data.datasource.api.JdbcCatalogMetadata;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Governance matrix for managed-only, BOOTSTRAP, and grandfather rules (D-13..D-17).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
class DatasourceGovernanceIT {

    @Test
    void devDraftSave_warnsOnInlineConnectionWithoutBlocking() {
        TemplateV2VO template = inlineJdbcTemplate();
        List<String> warnings = DatasourceGovernanceSupport.collectWarnings(
                template, stubCatalog(), true, true);
        Assertions.assertFalse(warnings.isEmpty());
    }

    @Test
    void stagingPublish_blocksInlineConnectionWhenManagedRequired() {
        TemplateV2VO template = inlineJdbcTemplate();
        org.gensokyo.data.config.DataGeneratorProperties.Governance governance =
                new org.gensokyo.data.config.DataGeneratorProperties.Governance();
        governance.setRequireManagedConnections(true);
        governance.setAllowBootstrapReferences(true);
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                TemplateV2Validator.validateGovernance(
                        template, false, governance, stubCatalog(), false));
    }

    @Test
    void prodRejectsBootstrapRefWhenNotAllowed() {
        TemplateV2VO template = managedJdbcTemplate("yaml-only-ds");
        org.gensokyo.data.config.DataGeneratorProperties.Governance governance =
                new org.gensokyo.data.config.DataGeneratorProperties.Governance();
        governance.setRequireManagedConnections(false);
        governance.setAllowBootstrapReferences(false);
        ConnectionCatalog catalog = bootstrapCatalog("yaml-only-ds");
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                TemplateV2Validator.validateGovernance(
                        template, false, governance, catalog, false));
    }

    @Test
    void grandfather_unchangedPublishedTemplateRunAllowsInline() {
        TemplateV2VO template = inlineJdbcTemplate();
        org.gensokyo.data.config.DataGeneratorProperties.Governance governance =
                new org.gensokyo.data.config.DataGeneratorProperties.Governance();
        governance.setRequireManagedConnections(true);
        governance.setAllowBootstrapReferences(false);
        Assertions.assertDoesNotThrow(() ->
                TemplateV2Validator.validateGovernance(
                        template, false, governance, stubCatalog(), true));
    }

    @Test
    void materialEdit_triggersEnforcementAfterGrandfather() {
        TemplateV2VO template = inlineJdbcTemplate();
        org.gensokyo.data.config.DataGeneratorProperties.Governance governance =
                new org.gensokyo.data.config.DataGeneratorProperties.Governance();
        governance.setRequireManagedConnections(true);
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                TemplateV2Validator.validateGovernance(
                        template, false, governance, stubCatalog(), false));
    }

    private static TemplateV2VO inlineJdbcTemplate() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("gov-inline");
        QuerySourceVO source = new QuerySourceVO();
        source.setType("query");
        source.setSql("select 1");
        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName("inline-orders");
        inline.setUrl("jdbc:h2:mem:inline-gov");
        inline.setDriverClassName("org.h2.Driver");
        inline.setPasswordSecretRef("secrets/db/demo");
        source.setDataSource(inline);
        template.setSources(new LinkedHashMap<>(Map.of("src", source)));
        return template;
    }

    private static TemplateV2VO managedJdbcTemplate(String dataSourceId) {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("gov-managed");
        QuerySourceVO source = new QuerySourceVO();
        source.setType("query");
        source.setSql("select 1");
        source.setDataSourceId(dataSourceId);
        template.setSources(new LinkedHashMap<>(Map.of("src", source)));
        return template;
    }

    private static ConnectionCatalog stubCatalog() {
        return new ConnectionCatalog() {
            @Override
            public org.gensokyo.data.datasource.api.ResolvedConnection resolve(String name, ConnectionKind kind) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<CatalogEntry> listAll() {
                return List.of();
            }

            @Override
            public ConnectionTestResult test(ConnectionTestRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CatalogEntry reload(String name, ConnectionKind kind) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<CatalogEntry> findEntry(String name, ConnectionKind kind) {
                return Optional.empty();
            }
        };
    }

    private static ConnectionCatalog bootstrapCatalog(String name) {
        CatalogEntry bootstrap = new CatalogEntry(
                name,
                ConnectionKind.JDBC,
                CatalogEntrySource.BOOTSTRAP,
                new JdbcCatalogMetadata("jdbc:h2:mem:bootstrap", "org.h2.Driver"),
                1L,
                Instant.now(),
                ConnectionHealthStatus.HEALTHY,
                null,
                null);
        return new ConnectionCatalog() {
            @Override
            public org.gensokyo.data.datasource.api.ResolvedConnection resolve(String n, ConnectionKind kind) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<CatalogEntry> listAll() {
                return List.of(bootstrap);
            }

            @Override
            public ConnectionTestResult test(ConnectionTestRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CatalogEntry reload(String n, ConnectionKind kind) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<CatalogEntry> findEntry(String n, ConnectionKind kind) {
                return n.equals(name) ? Optional.of(bootstrap) : Optional.empty();
            }
        };
    }
}
