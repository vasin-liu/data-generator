/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.ai.usage.AiPricingService;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.v2.MaskTransformVO;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformErrorVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link RunReportCollector#collectFailure} structured transform-error surfacing (D-08, D-10, D-13).
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
class RunReportCollectorFailureTests {

    private final RunReportCollector collector = new RunReportCollector(mock(AiPricingService.class));

    @Test
    void collectFailureDerivesStepPathAndOperatorTypeFromRuntimeWrapper() {
        MaskTransformVO mask = new MaskTransformVO();
        mask.setType("mask");
        mask.setName("pii-mask");
        TemplateV2VO template = new TemplateV2VO();
        template.setTransformers(List.of(mask));

        // Mirror TemplateV2RuntimeRegistry.runtimeFailure: IllegalStateException wrapping the factory cause.
        IllegalArgumentException cause = new IllegalArgumentException("Unknown mask strategy: rot13");
        IllegalStateException runtime = new IllegalStateException(
                "Failed to execute Template V2 transform factory [org.gensokyo.data.calcite.transform.MaskTransformFactory]"
                        + " for type [mask] and model [org.gensokyo.data.model.v2.MaskTransformVO]",
                cause);

        RunReportVO report = collector.collectFailure(template, runtime, 12L);

        assertThat(report).isNotNull();
        assertThat(report.transformErrors()).hasSize(1);
        TransformErrorVO error = report.transformErrors().getFirst();
        assertThat(error.operatorType()).isEqualTo("mask");
        assertThat(error.operatorName()).isEqualTo("pii-mask");
        assertThat(error.step()).isEqualTo("transformers[0]");
        assertThat(error.message()).isEqualTo("Unknown mask strategy: rot13");
        assertThat(report.errorSamples()).contains("Unknown mask strategy: rot13");
    }

    @Test
    void collectFailureFallsBackToTransformStepWhenTypeUnresolved() {
        RunReportVO report = collector.collectFailure(
                new TemplateV2VO(), new RuntimeException("boom"), 3L);

        assertThat(report).isNotNull();
        assertThat(report.transformErrors()).hasSize(1);
        TransformErrorVO error = report.transformErrors().getFirst();
        assertThat(error.step()).isEqualTo("transform");
        assertThat(error.operatorType()).isNull();
        assertThat(error.message()).isEqualTo("boom");
    }

    @Test
    void legacyReportJsonWithoutTransformErrorsDeserializesToEmptyList() {
        String legacyJson = "{\"sources\":[],\"transformers\":[],\"sinks\":[],"
                + "\"executionMode\":\"LOCAL\",\"durationMs\":5,\"errorSamples\":[],\"aiCalls\":[]}";

        RunReportVO report = TemplateJsonCodec.read(legacyJson, RunReportVO.class);

        assertThat(report.transformErrors()).isNotNull().isEmpty();
        assertThat(report.executionMode()).isEqualTo("LOCAL");
    }

    @Test
    void collectFailureMessageCarriesNoRawPiiValue() {
        MaskTransformVO mask = new MaskTransformVO();
        mask.setType("mask");
        TemplateV2VO template = new TemplateV2VO();
        template.setTransformers(List.of(mask));

        // The mask factory already sanitizes — the cause never echoes the raw column value.
        IllegalStateException runtime = new IllegalStateException(
                "Failed to execute Template V2 transform factory [Mask] for type [mask] and model [MaskTransformVO]",
                new IllegalArgumentException("Unknown mask strategy: rot13"));

        RunReportVO report = collector.collectFailure(template, runtime, 1L);

        TransformErrorVO error = report.transformErrors().getFirst();
        assertThat(error.message()).doesNotContain("@");
        assertThat(error.message()).isEqualTo("Unknown mask strategy: rot13");
    }
}
