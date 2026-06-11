/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates and executes greenfield workflow scenario YAML under {@code template/v2-scenarios/}.
 *
 * @author Gensokyo
 * @version 1.0.0
 * @since 2026-05-29
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class V2WorkflowScenarioIT {

    private static final String SCENARIO_ROOT = "template/v2-scenarios/";

    @Autowired
    private TemplateV2Runner templateV2Runner;

    /**
     * Workflow scenario fixtures under {@code template/v2-scenarios/}.
     *
     * @return relative classpath paths for each workflow scenario YAML
     */
    static Stream<String> scenarioResources() {
        return Stream.of(
                "scenario-wf-pause-log.yaml",
                "scenario-wf-branch.yaml",
                "scenario-dag-join.yaml",
                "scenario-dag-fanout.yaml",
                "scenario-js-transform.yaml",
                "scenario-spel-transform.yaml",
                "scenario-wf-shared-state.yaml"
        ).map(name -> SCENARIO_ROOT + name);
    }

    /**
     * Loads each workflow scenario, validates structure, and runs end to end with outcome assertions.
     *
     * @param resourcePath classpath path to scenario YAML
     * @throws Exception when fixture setup or execution fails
     */
    @ParameterizedTest
    @MethodSource("scenarioResources")
    void validatesAndRunsWorkflowScenario(String resourcePath) throws Exception {
        TemplateV2VO template = loadTemplate(resourcePath);
        TemplateV2Validator.validate(template);

        TemplateV2RunResult result = templateV2Runner.run(template);

        assertScenarioOutcome(template.getName(), result);
    }

    private TemplateV2VO loadTemplate(String resourcePath) throws IOException {
        String yaml = readClasspath(resourcePath);
        TemplateV2VO template = new JacksonParser().parse(yaml, TemplateV2VO.class);
        assertThat(template).isNotNull();
        assertThat(template.getName()).isNotBlank();
        assertThat(template.getWorkflow()).isNotNull();
        return template;
    }

    private void assertScenarioOutcome(String scenarioName, TemplateV2RunResult result) {
        assertThat(result).isNotNull();
        assertThat(result.getMetrics()).isNotNull();
        List<String> warnings = result.getMetrics().getWarnings();

        switch (scenarioName) {
            case "scenario-wf-pause-log" -> {
                assertThat(warnings).hasSizeGreaterThanOrEqualTo(2);
                assertThat(warnings.get(0)).contains("workflow-start");
                assertThat(warnings.get(1)).contains("workflow-end");
                assertThat(result.getRows()).hasSize(3);
                assertThat(result.getRows().getFirst().getString("value")).isEqualTo("1");
            }
            case "scenario-wf-branch" -> {
                assertThat(warnings).anyMatch(entry -> entry.contains("then-branch"));
                assertThat(warnings).noneMatch(entry -> entry.contains("else-branch"));
                assertThat(result.getRows()).hasSize(2);
            }
            case "scenario-dag-join" -> {
                assertThat(result.getRows()).hasSize(2);
                assertThat(result.getRows().get(0).getString("value")).isEqualTo("4");
                assertThat(result.getRows().get(0).getString("shifted")).isEqualTo("14");
                assertThat(result.getRows().get(1).getString("value")).isEqualTo("5");
                assertThat(result.getRows().get(1).getString("shifted")).isEqualTo("15");
            }
            case "scenario-dag-fanout" -> {
                assertThat(result.getRows()).hasSize(3);
                assertThat(toLong(result.getRows().get(0).values().get("value"))).isEqualTo(1L);
                assertThat(toLong(result.getRows().get(0).values().get("doubled"))).isEqualTo(2L);
                assertThat(toLong(result.getRows().get(1).values().get("value"))).isEqualTo(2L);
                assertThat(toLong(result.getRows().get(1).values().get("doubled"))).isEqualTo(4L);
                assertThat(toLong(result.getRows().get(2).values().get("value"))).isEqualTo(3L);
                assertThat(toLong(result.getRows().get(2).values().get("doubled"))).isEqualTo(6L);
            }
            case "scenario-wf-shared-state" -> {
                assertThat(warnings).anyMatch(entry -> entry.contains("shared-state-ok"));
                assertThat(warnings).noneMatch(entry -> entry.contains("shared-state-fail"));
                assertThat(result.getRows()).hasSize(3);
            }
            case "scenario-js-transform" -> {
                assertThat(result.getRows()).hasSize(3);
                assertThat(toLong(result.getRows().get(0).values().get("amount"))).isEqualTo(2L);
                assertThat(toLong(result.getRows().get(1).values().get("amount"))).isEqualTo(4L);
                assertThat(toLong(result.getRows().get(2).values().get("amount"))).isEqualTo(6L);
            }
            case "scenario-spel-transform" -> {
                assertThat(result.getRows()).hasSize(3);
                assertThat(toLong(result.getRows().get(0).values().get("doubled"))).isEqualTo(2L);
                assertThat(toLong(result.getRows().get(1).values().get("doubled"))).isEqualTo(4L);
                assertThat(toLong(result.getRows().get(2).values().get("doubled"))).isEqualTo(6L);
            }
            default -> throw new IllegalArgumentException("Unknown workflow scenario: " + scenarioName);
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value).split("\\.")[0]);
    }

    private static String readClasspath(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
