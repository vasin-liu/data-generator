/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link UdfRegistryService} backed by {@link InMemoryUdfRegistry}.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
class UdfRegistryServiceTests {

    private UdfRegistryService service;

    @BeforeEach
    void setUp() {
        service = new UdfRegistryService(new InMemoryUdfRegistry());
    }

    @Test
    void registerPublishResolveRoundTrip() {
        service.registerDraft(
                "com.example.echo",
                "1.0.0",
                UdfType.SCRIPT,
                "function udf(input) { return input; }".getBytes(StandardCharsets.UTF_8),
                Map.of());
        service.publish("com.example.echo", "1.0.0");

        UdfRecord resolved = service.resolve("com.example.echo", Optional.empty());
        Assertions.assertEquals(UdfLifecycleState.PUBLISHED, resolved.state());
        Assertions.assertEquals("1.0.0", resolved.version());
    }

    @Test
    void rejectsDuplicateUdfIdAndVersion() {
        byte[] payload = "x".getBytes(StandardCharsets.UTF_8);
        service.registerDraft("com.example.dup", "1.0.0", UdfType.SQL, payload, Map.of());

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                service.registerDraft("com.example.dup", "1.0.0", UdfType.SQL, payload, Map.of()));
        Assertions.assertEquals("UDF_DUPLICATE_VERSION", ex.code());
    }

    @Test
    void resolveFailsForDraft() {
        service.registerDraft("com.example.draft", "1.0.0", UdfType.SQL, new byte[0], Map.of());

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                service.resolve("com.example.draft", Optional.of("1.0.0")));
        Assertions.assertEquals("UDF_NOT_PUBLISHED", ex.code());
    }

    @Test
    void resolveFailsForDeprecated() {
        service.registerDraft("com.example.old", "1.0.0", UdfType.SQL, new byte[0], Map.of());
        service.publish("com.example.old", "1.0.0");
        service.deprecate("com.example.old", "1.0.0");

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                service.resolve("com.example.old", Optional.of("1.0.0")));
        Assertions.assertEquals("UDF_DEPRECATED", ex.code());
    }

    @Test
    void resolveLatestPublishedVersion() {
        service.registerDraft("com.example.versioned", "1.0.0", UdfType.SQL, "a".getBytes(StandardCharsets.UTF_8), Map.of());
        service.publish("com.example.versioned", "1.0.0");
        service.registerDraft("com.example.versioned", "1.1.0", UdfType.SQL, "b".getBytes(StandardCharsets.UTF_8), Map.of());
        service.publish("com.example.versioned", "1.1.0");

        UdfRecord latest = service.resolve("com.example.versioned", Optional.empty());
        Assertions.assertEquals("1.1.0", latest.version());
    }
}
