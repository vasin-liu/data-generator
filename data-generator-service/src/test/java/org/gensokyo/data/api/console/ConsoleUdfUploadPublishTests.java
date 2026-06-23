/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.api.console.dto.UdfVersionView;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.udf.UdfRegistryException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the console upload path produces an artifact the publish gate accepts — i.e. the controller
 * assembles the {@code ScriptUdfPayload} envelope, not the raw body. This is the regression guard for the
 * upload→publish contract that the service-path E2E cannot catch because it bypasses the controller.
 *
 * <p>Shares the {@code application-phase7-test.yaml} context with the other Phase 3 service tests so no
 * extra Spring context is started.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class ConsoleUdfUploadPublishTests {

    private static final String MASK_SCRIPT =
            "return String(args[0]).replace(/^(.).*(@.*)$/, '$1***$2');";
    private static final String NORMALIZE_SCRIPT = "return String(args[0]).replace(/[^0-9]/g, '');";
    private static final String STRING_SCHEMA = "{\"type\":\"string\"}";

    @Autowired
    private ConsoleUdfController controller;

    @Test
    void sqlUpload_thenPublish_succeeds() throws Exception {
        // SQL upload carries no schemas; the controller envelope must still be publishable.
        controller.upload("com.example.console.sqlmask", "1.0.0", "sql",
                null, null, MASK_SCRIPT, "V2_CONSOLE_MASK", null, "VARCHAR", null, null);

        R<UdfVersionView> published = controller.publish("com.example.console.sqlmask", "1.0.0");

        assertThat(published.getData().state()).isEqualTo("published");
    }

    @Test
    void scriptUpload_withSchemas_thenPublish_succeeds() throws Exception {
        // SCRIPT publish additionally requires non-empty input/output JSON Schemas (D-12).
        controller.upload("com.example.console.scriptphone", "1.0.0", "script",
                null, NORMALIZE_SCRIPT, null, "V2_CONSOLE_PHONE", null, "VARCHAR", STRING_SCHEMA, STRING_SCHEMA);

        R<UdfVersionView> published = controller.publish("com.example.console.scriptphone", "1.0.0");

        assertThat(published.getData().state()).isEqualTo("published");
    }

    @Test
    void scriptUpload_withoutSchemas_failsGovernanceAtPublish() throws Exception {
        // Omitting the schemas yields a valid draft but a governance rejection at publish (D-12).
        controller.upload("com.example.console.scriptnoschema", "1.0.0", "script",
                null, NORMALIZE_SCRIPT, null, "V2_CONSOLE_NOSCHEMA", null, "VARCHAR", null, null);

        assertThatThrownBy(() -> controller.publish("com.example.console.scriptnoschema", "1.0.0"))
                .isInstanceOf(UdfRegistryException.class)
                .hasMessageContaining("governance");
    }
}
