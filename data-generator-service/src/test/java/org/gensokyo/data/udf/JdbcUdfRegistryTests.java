/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.repository.UdfArtifactRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Integration tests for {@link JdbcUdfRegistry}: persistence, duplicate guard, lifecycle, resolution, and the
 * active-bean assertion proving the JDBC-backed registry replaced the in-memory default (D-01/D-04/D-08).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class JdbcUdfRegistryTests {

    @Autowired
    private UdfArtifactRepository repository;

    @Autowired
    private UdfRegistry udfRegistry;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void injectedRegistryBeanIsJdbcBacked() {
        // The conditional in-memory default backs off to the JDBC-backed registry (D-01).
        Assertions.assertInstanceOf(JdbcUdfRegistry.class, udfRegistry);
    }

    @Test
    void rejectsDuplicateUdfIdAndVersion() {
        JdbcUdfRegistry registry = new JdbcUdfRegistry(repository);
        byte[] payload = "x".getBytes(StandardCharsets.UTF_8);
        registry.registerDraft("com.example.dup", "1.0.0", UdfType.SQL, payload, Map.of());

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                registry.registerDraft("com.example.dup", "1.0.0", UdfType.SQL, payload, Map.of()));
        Assertions.assertEquals("UDF_DUPLICATE_VERSION", ex.code());
    }

    @Test
    void resolveLatestPublishedVersion() {
        JdbcUdfRegistry registry = new JdbcUdfRegistry(repository);
        registry.registerDraft("com.example.versioned", "1.0.0", UdfType.SQL,
                "a".getBytes(StandardCharsets.UTF_8), Map.of("sqlName", "V2_A"));
        registry.publish("com.example.versioned", "1.0.0");
        registry.registerDraft("com.example.versioned", "1.1.0", UdfType.SQL,
                "b".getBytes(StandardCharsets.UTF_8), Map.of("sqlName", "V2_B"));
        registry.publish("com.example.versioned", "1.1.0");

        UdfRecord latest = registry.resolve("com.example.versioned", Optional.empty());
        Assertions.assertEquals("1.1.0", latest.version());
        Assertions.assertEquals(UdfLifecycleState.PUBLISHED, latest.state());
        // Metadata round-trips through the metadata_json CLOB column.
        Assertions.assertEquals("V2_B", latest.metadata().get("sqlName"));
    }

    @Test
    void resolveFailsForDraft() {
        JdbcUdfRegistry registry = new JdbcUdfRegistry(repository);
        registry.registerDraft("com.example.draft", "1.0.0", UdfType.SQL, new byte[0], Map.of());

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                registry.resolve("com.example.draft", Optional.of("1.0.0")));
        Assertions.assertEquals("UDF_NOT_PUBLISHED", ex.code());
    }

    @Test
    void resolveFailsForDeprecated() {
        JdbcUdfRegistry registry = new JdbcUdfRegistry(repository);
        registry.registerDraft("com.example.old", "1.0.0", UdfType.SQL, new byte[0], Map.of());
        registry.publish("com.example.old", "1.0.0");
        registry.deprecate("com.example.old", "1.0.0");

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                registry.resolve("com.example.old", Optional.of("1.0.0")));
        Assertions.assertEquals("UDF_DEPRECATED", ex.code());
    }

    @Test
    void rowSurvivesAcrossRegistryInstances() {
        // A row written through one registry instance is re-read by a fresh instance over the same
        // datasource — proving persistence is durable, not in-process (D-01 truth #1).
        JdbcUdfRegistry writer = new JdbcUdfRegistry(repository);
        writer.registerDraft("com.example.durable", "2.3.4", UdfType.SCRIPT,
                "function udf(x){return x;}".getBytes(StandardCharsets.UTF_8), Map.of());

        JdbcUdfRegistry reader = new JdbcUdfRegistry(repository);
        Optional<UdfRecord> reloaded = reader.find("com.example.durable", "2.3.4");
        Assertions.assertTrue(reloaded.isPresent());
        Assertions.assertEquals(UdfType.SCRIPT, reloaded.get().type());
        Assertions.assertEquals(UdfLifecycleState.DRAFT, reloaded.get().state());
    }
}
