/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.calcite.TemplateV2SqlFunction;
import org.gensokyo.data.calcite.TemplateV2SqlFunctionContext;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistryFactory;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeContext;
import org.gensokyo.data.calcite.runtime.RefreshableTemplateV2RuntimeRegistryProvider;
import org.gensokyo.data.calcite.udf.GraalJsScriptUdfExecutor;
import org.gensokyo.data.calcite.udf.RegistryBackedRuntimePluginProvider;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link UdfPublishService}: publish gate, governance, audit, and runtime refresh.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
class UdfPublishServiceTests {

    private InMemoryUdfRegistry registry;
    private AuditService auditService;
    private DefaultRegistrySqlFunctionSource functionSource;
    private RegistryBackedRuntimePluginProvider pluginProvider;
    private UdfPublishService publishService;

    @BeforeEach
    void setUp() {
        registry = new InMemoryUdfRegistry();
        auditService = Mockito.mock(AuditService.class);
        GraalJsScriptUdfExecutor executor = new GraalJsScriptUdfExecutor();
        functionSource = new DefaultRegistrySqlFunctionSource(registry, executor);
        pluginProvider = new RegistryBackedRuntimePluginProvider(functionSource);
        // Real refreshable provider proves publish triggers a runtime rebuild (D-08).
        RefreshableTemplateV2RuntimeRegistryProvider runtimeProvider =
                new RefreshableTemplateV2RuntimeRegistryProvider(List.of(pluginProvider),
                        new TemplateV2RuntimeRegistryFactory(), TemplateV2RuntimeContext.empty());
        publishService = new UdfPublishService(registry, auditService, runtimeProvider, new DataGeneratorProperties());
    }

    @Test
    void publishSqlUdfMakesItResolvableAndAudits() {
        String payload = "{\"sqlName\":\"V2_REG_GREET\",\"argCount\":1,\"returnType\":\"VARCHAR\","
                + "\"script\":\"return 'hi ' + args[0];\"}";
        registry.registerDraft("com.example.greet", "1.0.0", UdfType.SQL,
                payload.getBytes(StandardCharsets.UTF_8), Map.of());

        publishService.publish("com.example.greet", "1.0.0");

        Mockito.verify(auditService).record(Mockito.eq("UDF_PUBLISH"), Mockito.eq("udf"),
                Mockito.eq("com.example.greet@1.0.0"), Mockito.anyMap());
        TemplateV2SqlFunction greet = findFunction("V2_REG_GREET");
        Object result = greet.evaluator().evaluate(new TemplateV2SqlFunctionContext(List.of("bob")));
        Assertions.assertEquals("hi bob", result);
    }

    @Test
    void governanceRejectsForbiddenScriptToken() {
        String payload = "{\"sqlName\":\"V2_BAD\",\"argCount\":0,\"returnType\":\"VARCHAR\","
                + "\"script\":\"return java.lang.System.lineSeparator();\"}";
        registry.registerDraft("com.example.bad", "1.0.0", UdfType.SQL,
                payload.getBytes(StandardCharsets.UTF_8), Map.of());

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                publishService.publish("com.example.bad", "1.0.0"));
        Assertions.assertEquals("UDF_GOVERNANCE_VIOLATION", ex.code());
        Assertions.assertTrue(ex.errors().stream()
                .anyMatch(e -> "UDF_SCRIPT_FORBIDDEN_PATTERN".equals(e.code())));
        // Draft must remain unpublished after a governance failure.
        Assertions.assertEquals(UdfLifecycleState.DRAFT,
                registry.find("com.example.bad", "1.0.0").orElseThrow().state());
    }

    @Test
    void governanceRejectsScriptUdfMissingSchema() {
        String payload = "{\"sqlName\":\"V2_NOSCHEMA\",\"argCount\":1,\"returnType\":\"VARCHAR\","
                + "\"script\":\"return args[0];\"}";
        registry.registerDraft("com.example.noschema", "1.0.0", UdfType.SCRIPT,
                payload.getBytes(StandardCharsets.UTF_8), Map.of());

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                publishService.publish("com.example.noschema", "1.0.0"));
        Assertions.assertTrue(ex.errors().stream().anyMatch(e -> "UDF_SCHEMA_MISSING".equals(e.code())));
    }

    @Test
    void governanceRejectsPlaintextSecret() {
        String payload = "{\"sqlName\":\"V2_SECRET\",\"argCount\":0,\"returnType\":\"VARCHAR\","
                + "\"password\":\"hunter2\",\"script\":\"return 'x';\"}";
        registry.registerDraft("com.example.secret", "1.0.0", UdfType.SQL,
                payload.getBytes(StandardCharsets.UTF_8), Map.of());

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class, () ->
                publishService.publish("com.example.secret", "1.0.0"));
        Assertions.assertTrue(ex.errors().stream().anyMatch(e -> "UDF_SECRET_PLAINTEXT".equals(e.code())));
    }

    @Test
    void builtInFunctionNameCollisionIsSkipped() {
        String payload = "{\"sqlName\":\"V2_TO_DATE\",\"argCount\":1,\"returnType\":\"VARCHAR\","
                + "\"script\":\"return args[0];\"}";
        registry.registerDraft("com.example.collide", "1.0.0", UdfType.SQL,
                payload.getBytes(StandardCharsets.UTF_8), Map.of());

        publishService.publish("com.example.collide", "1.0.0");

        // Built-in V2_TO_DATE wins; the registry entry is filtered from the contributed plugin (D-07).
        boolean contributed = pluginProvider.createPlugin(TemplateV2RuntimeContext.empty()).sqlFunctions().stream()
                .anyMatch(f -> "V2_TO_DATE".equals(f.name()));
        Assertions.assertFalse(contributed);
    }

    @Test
    void deprecateRemovesFunctionAndAudits() {
        String payload = "{\"sqlName\":\"V2_REG_TEMP\",\"argCount\":0,\"returnType\":\"VARCHAR\","
                + "\"script\":\"return 'temp';\"}";
        registry.registerDraft("com.example.temp", "1.0.0", UdfType.SQL,
                payload.getBytes(StandardCharsets.UTF_8), Map.of());
        publishService.publish("com.example.temp", "1.0.0");

        publishService.deprecate("com.example.temp", "1.0.0");

        Mockito.verify(auditService).record(Mockito.eq("UDF_DEPRECATE"), Mockito.eq("udf"),
                Mockito.eq("com.example.temp@1.0.0"), Mockito.anyMap());
        Assertions.assertTrue(functionSource.publishedSqlFunctions().stream()
                .noneMatch(f -> "V2_REG_TEMP".equals(f.name())));
    }

    private TemplateV2SqlFunction findFunction(String name) {
        return functionSource.publishedSqlFunctions().stream()
                .filter(f -> name.equals(f.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Function not found: " + name));
    }
}
