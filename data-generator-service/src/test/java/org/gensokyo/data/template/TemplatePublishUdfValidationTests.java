/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.udf.UdfRegistryException;
import org.gensokyo.data.udf.UdfRegistryService;
import org.gensokyo.data.udf.UdfType;
import org.gensokyo.data.util.RandomKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Publish-gate tests proving template publish hard-fails on unknown UDF references while draft saves stay lenient
 * (UDF-06, D-09/D-11/D-12). Runs against the embedded H2 service context.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class TemplatePublishUdfValidationTests {

    private static final String YAML_TEMPLATE = """
            name: %s
            sources:
              input:
                type: iterator
                iterator:
                  type: number
                  from: 1
                  to: 5
                  step: 1
            transform:
              type: sql
              sql: SELECT V2_GREET(value) AS g FROM input
            sink:
              writers:
                - type: console
            """;

    @Autowired
    private TemplateLifecycleService templateLifecycleService;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UdfRegistryService udfRegistryService;

    @Test
    @Transactional
    void publishWithUnknownUdfReferenceFailsNotFound() {
        TemplatePO row = newDraftTemplate("publish-unknown-udf");
        templateRepository.saveAndFlush(row);

        UdfRegistryException ex = Assertions.assertThrows(UdfRegistryException.class,
                () -> templateLifecycleService.publish(row.getId()));
        Assertions.assertEquals("UDF_NOT_FOUND", ex.code());
    }

    @Test
    @Transactional
    void publishWithPublishedUdfReferenceSucceeds() {
        // sqlName lives in the payload envelope — the same source the Calcite runtime registers from.
        byte[] payload = "{\"sqlName\":\"V2_GREET\",\"argCount\":1,\"returnType\":\"VARCHAR\",\"script\":\"return args[0];\"}"
                .getBytes(StandardCharsets.UTF_8);
        udfRegistryService.registerDraft("com.example.greet", "1.0.0", UdfType.SQL, payload, Map.of());
        udfRegistryService.publish("com.example.greet", "1.0.0");

        TemplatePO row = newDraftTemplate("publish-known-udf");
        templateRepository.saveAndFlush(row);

        Assertions.assertDoesNotThrow(() -> templateLifecycleService.publish(row.getId()));
        Assertions.assertEquals(TemplateLifecycleStatus.PUBLISHED.name(),
                templateRepository.findById(row.getId()).orElseThrow().getStatus());
    }

    @Test
    @Transactional
    void draftSaveWithDanglingReferenceIsLenient() {
        // No UDF reference validation runs on the draft-save path (D-11), so the dangling V2_GREET token persists.
        TemplatePO row = newDraftTemplate("draft-dangling-udf");
        Assertions.assertDoesNotThrow(() -> templateRepository.saveAndFlush(row));
        Assertions.assertEquals(TemplateLifecycleStatus.DRAFT.name(),
                templateRepository.findById(row.getId()).orElseThrow().getStatus());
    }

    private static TemplatePO newDraftTemplate(String name) {
        TemplatePO row = new TemplatePO();
        row.setId(RandomKit.snowFlake().nextId());
        row.setName(name);
        row.setArchived(Boolean.FALSE);
        row.setStatus(TemplateLifecycleStatus.DRAFT.name());
        row.setContentYaml(YAML_TEMPLATE.formatted(name));
        return row;
    }
}
