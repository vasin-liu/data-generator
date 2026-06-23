/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof of the Phase 3 loop for the in-repo sample UDFs (UDF-08, D-19): register a draft,
 * publish through the governance/persistence service path, reference the published UDF from a Template V2
 * SQL transform, run it against embedded H2, and assert the transformed output.
 *
 * <p>The script and sql samples both surface to Calcite as SQL-callable functions ({@code V2_FORMAT_PHONE}
 * / {@code V2_MASK_EMAIL}); publishing refreshes the runtime registry (D-08) so the autowired
 * {@link TemplateV2Runner} resolves them at run time.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class UdfConsoleTemplateBindingE2ETests {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String TEMPLATE_YAML = """
            name: udf-binding-e2e
            sources:
              people:
                type: inline_rows
                rows:
                  - id: 1
                    email: alice@example.com
                    phone: "+1 (555) 123-4567"
            transform:
              type: sql
              sql: SELECT id, V2_MASK_EMAIL(email) AS masked_email, V2_FORMAT_PHONE(phone) AS clean_phone FROM people
            sink:
              writers:
                - type: console
            """;

    @Autowired
    private UdfRegistryService udfRegistryService;

    @Autowired
    private UdfPublishService udfPublishService;

    @Autowired
    private TemplateV2Runner templateV2Runner;

    @Test
    void registerPublishReferenceRunForSampleUdfs() throws IOException {
        // SQL sample: GraalJS-backed SQL-callable function; no JSON Schema required.
        byte[] maskPayload = scriptPayload("V2_MASK_EMAIL", readSample("mask-email.sql"), null);
        udfRegistryService.registerDraft("com.example.udf.maskemail", "1.0.0", UdfType.SQL, maskPayload, Map.of());
        udfPublishService.publish("com.example.udf.maskemail", "1.0.0");

        // Script sample: SCRIPT type requires non-empty input/output JSON Schema at publish (D-12).
        byte[] phonePayload = scriptPayload(
                "V2_FORMAT_PHONE", readSample("format-phone.js"), readSample("format-phone.schema.json"));
        udfRegistryService.registerDraft(
                "com.example.udf.formatphone", "1.0.0", UdfType.SCRIPT, phonePayload, Map.of());
        udfPublishService.publish("com.example.udf.formatphone", "1.0.0");

        TemplateV2DraftVO draft = new JacksonParser().parse(TEMPLATE_YAML, TemplateV2DraftVO.class);
        TemplateV2VO template = TemplateV2Normalizer.normalize(draft);

        TemplateV2RunResult result = templateV2Runner.run(template);

        assertThat(result).isNotNull();
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().getFirst().getString("masked_email")).isEqualTo("a***@example.com");
        assertThat(result.getRows().getFirst().getString("clean_phone")).isEqualTo("15551234567");
    }

    private static byte[] scriptPayload(String sqlName, String script, String schemaJson) {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("sqlName", sqlName);
        payload.put("argCount", 1);
        payload.put("returnType", "VARCHAR");
        payload.put("script", script);
        if (schemaJson != null) {
            // SCRIPT UDFs carry input/output JSON Schema; the sample file holds them under input/output.
            JsonNode schema = MAPPER.readTree(schemaJson);
            payload.set("inputSchema", schema.get("input"));
            payload.set("outputSchema", schema.get("output"));
        }
        return MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
    }

    private static String readSample(String name) throws IOException {
        return Files.readString(samplesDir().resolve(name), StandardCharsets.UTF_8);
    }

    private static Path samplesDir() {
        // Resolve samples/udf-samples relative to the working dir, walking up to the repo root if needed.
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("samples").resolve("udf-samples");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "samples/udf-samples not found from " + System.getProperty("user.dir"));
    }
}
