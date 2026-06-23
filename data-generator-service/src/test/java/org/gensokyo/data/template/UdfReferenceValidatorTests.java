/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.JsTransformVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.udf.InMemoryUdfRegistry;
import org.gensokyo.data.udf.UdfRegistryException;
import org.gensokyo.data.udf.UdfRegistryService;
import org.gensokyo.data.udf.UdfType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link UdfReferenceValidator}: SQL {@code sqlName} + script {@code udfRef} resolution against a
 * real {@link UdfRegistryService} over {@link InMemoryUdfRegistry}, covering the Phase 2 structured codes and the
 * built-in allow-list (UDF-06, D-09/D-10/D-12).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
class UdfReferenceValidatorTests {

    private UdfRegistryService registryService;
    private UdfReferenceValidator validator;

    @BeforeEach
    void setUp() {
        registryService = new UdfRegistryService(new InMemoryUdfRegistry());
        validator = new UdfReferenceValidator(registryService);
    }

    @Test
    void publishedScriptReferencePasses() {
        registryService.registerDraft("com.example.script", "1.0.0", UdfType.SCRIPT,
                "x".getBytes(StandardCharsets.UTF_8), Map.of());
        registryService.publish("com.example.script", "1.0.0");

        TemplateV2VO template = templateWithScript(
                "const r = udfRef:{ id: \"com.example.script\", version: \"1.0.0\" }; return r;");
        Assertions.assertDoesNotThrow(() -> validator.validate(template));
    }

    @Test
    void unknownScriptReferenceFailsNotFound() {
        TemplateV2VO template = templateWithScript("udfRef:{ id: \"com.example.missing\" }");

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class,
                () -> validator.validate(template));
        Assertions.assertEquals("UDF_NOT_FOUND", ex.code());
    }

    @Test
    void draftScriptReferenceFailsNotPublished() {
        registryService.registerDraft("com.example.draft", "1.0.0", UdfType.SCRIPT,
                "x".getBytes(StandardCharsets.UTF_8), Map.of());

        TemplateV2VO template = templateWithScript(
                "udfRef:{ id: \"com.example.draft\", version: \"1.0.0\" }");

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class,
                () -> validator.validate(template));
        Assertions.assertEquals("UDF_NOT_PUBLISHED", ex.code());
    }

    @Test
    void deprecatedScriptReferenceFailsDeprecated() {
        registryService.registerDraft("com.example.old", "1.0.0", UdfType.SCRIPT,
                "x".getBytes(StandardCharsets.UTF_8), Map.of());
        registryService.publish("com.example.old", "1.0.0");
        registryService.deprecate("com.example.old", "1.0.0");

        TemplateV2VO template = templateWithScript(
                "udfRef:{ id: \"com.example.old\", version: \"1.0.0\" }");

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class,
                () -> validator.validate(template));
        Assertions.assertEquals("UDF_DEPRECATED", ex.code());
    }

    @Test
    void publishedSqlNameReferencePasses() {
        // sqlName lives in the payload envelope — the same source the Calcite runtime registers from.
        byte[] payload = "{\"sqlName\":\"V2_GREET\",\"argCount\":1,\"returnType\":\"VARCHAR\",\"script\":\"return args[0];\"}"
                .getBytes(StandardCharsets.UTF_8);
        registryService.registerDraft("com.example.greet", "1.0.0", UdfType.SQL, payload, Map.of());
        registryService.publish("com.example.greet", "1.0.0");

        TemplateV2VO template = templateWithSql("SELECT V2_GREET(name) AS g FROM input");
        Assertions.assertDoesNotThrow(() -> validator.validate(template));
    }

    @Test
    void unknownSqlFunctionTokenFailsNotFound() {
        TemplateV2VO template = templateWithSql("SELECT V2_UNKNOWN(x) FROM input");

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class,
                () -> validator.validate(template));
        Assertions.assertEquals("UDF_NOT_FOUND", ex.code());
    }

    @Test
    void builtInSqlFunctionIsNotFlagged() {
        TemplateV2VO template = templateWithSql("SELECT COUNT(*) AS c FROM input");
        Assertions.assertDoesNotThrow(() -> validator.validate(template));
    }

    private static TemplateV2VO templateWithSql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return templateWithTransform(transform);
    }

    private static TemplateV2VO templateWithScript(String script) {
        JsTransformVO transform = new JsTransformVO();
        transform.setScript(script);
        return templateWithTransform(transform);
    }

    private static TemplateV2VO templateWithTransform(TransformVO transform) {
        TemplateV2VO template = new TemplateV2VO();
        template.setTransformers(List.of(transform));
        return template;
    }
}
