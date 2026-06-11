/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2.workflow;

import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Verifies workflow step polymorphic serialization through YAML and JSON codecs.
 */
class WorkflowSpecVOSerializationTests {

    @Test
    void roundTripsWorkflowSpecViaYaml() {
        WorkflowSpecVO spec = sampleWorkflowSpec();
        JacksonParser parser = new JacksonParser();

        String yaml = parser.dump(wrapTemplate(spec));
        TemplateV2VO decoded = parser.parse(yaml, TemplateV2VO.class);

        assertWorkflowSpec(decoded.getWorkflow());
    }

    @Test
    void roundTripsWorkflowSpecViaJson() {
        WorkflowSpecVO spec = sampleWorkflowSpec();

        String json = TemplateJsonCodec.write(wrapTemplate(spec));
        TemplateV2VO decoded = TemplateJsonCodec.read(json, TemplateV2VO.class);

        assertWorkflowSpec(decoded.getWorkflow());
    }

    private static TemplateV2VO wrapTemplate(WorkflowSpecVO workflow) {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("workflow-serde");
        template.setWorkflow(workflow);
        return template;
    }

    private static WorkflowSpecVO sampleWorkflowSpec() {
        PauseStepVO pause = new PauseStepVO();
        pause.setId("pause-1");
        pause.setName("wait-before-export");
        pause.setDurationMs(500L);

        InvokeComputeBlockStepVO invoke = new InvokeComputeBlockStepVO();
        invoke.setId("invoke-1");
        invoke.setName("run-export-block");
        invoke.setComputeBlockId("export-block");

        WorkflowSpecVO spec = new WorkflowSpecVO();
        spec.setSteps(List.of(pause, invoke));
        return spec;
    }

    private static void assertWorkflowSpec(WorkflowSpecVO workflow) {
        Assertions.assertNotNull(workflow);
        Assertions.assertEquals(2, workflow.getSteps().size());

        Assertions.assertInstanceOf(PauseStepVO.class, workflow.getSteps().get(0));
        PauseStepVO pause = (PauseStepVO) workflow.getSteps().get(0);
        Assertions.assertEquals("pause", pause.getType());
        Assertions.assertEquals("pause-1", pause.getId());
        Assertions.assertEquals(500L, pause.getDurationMs());

        Assertions.assertInstanceOf(InvokeComputeBlockStepVO.class, workflow.getSteps().get(1));
        InvokeComputeBlockStepVO invoke = (InvokeComputeBlockStepVO) workflow.getSteps().get(1);
        Assertions.assertEquals("invoke_compute_block", invoke.getType());
        Assertions.assertEquals("export-block", invoke.getComputeBlockId());
    }
}
