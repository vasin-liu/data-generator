/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StageMetricVO} extended sink counters and JSON compatibility (RW-04, D-16).
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
class StageMetricVOTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extended sink counters round-trip through JSON with explicit keys.
     */
    @Test
    void jsonRoundTripIncludesExtendedSinkCounters() throws Exception {
        StageMetricVO metric = new StageMetricVO(
                "sink[0].writer[0]",
                10L,
                null,
                null,
                9L,
                1L,
                10L,
                3L,
                1L);

        String json = objectMapper.writeValueAsString(metric);
        assertThat(json).contains("\"rowsUpserted\":3");
        assertThat(json).contains("\"rowsSkipped\":1");
        assertThat(json).contains("\"rowsRead\":10");

        StageMetricVO decoded = objectMapper.readValue(json, StageMetricVO.class);
        assertThat(decoded.rowsUpserted()).isEqualTo(3L);
        assertThat(decoded.rowsSkipped()).isEqualTo(1L);
        assertThat(decoded.rowsRead()).isEqualTo(10L);
    }

    /**
     * Legacy JSON without extended fields deserializes with zero defaults.
     */
    @Test
    void legacyJsonWithoutExtendedFieldsDeserializesToZeroDefaults() throws Exception {
        String legacyJson = "{\"name\":\"sink[0]\",\"rowsProcessed\":5,\"durationMs\":null,"
                + "\"errorSample\":null,\"rowsOk\":5,\"rowsFailed\":0}";

        StageMetricVO decoded = objectMapper.readValue(legacyJson, StageMetricVO.class);

        assertThat(decoded.rowsRead()).isZero();
        assertThat(decoded.rowsUpserted()).isZero();
        assertThat(decoded.rowsSkipped()).isZero();
        assertThat(decoded.rowsOk()).isEqualTo(5L);
    }

    /**
     * Six-argument constructor remains available for older call sites.
     */
    @Test
    void legacyConstructorNormalizesExtendedCountersToZero() {
        StageMetricVO metric = new StageMetricVO("sink[0]", 4L, null, null, 4L, 0L);

        assertThat(metric.rowsRead()).isZero();
        assertThat(metric.rowsUpserted()).isZero();
        assertThat(metric.rowsSkipped()).isZero();
    }
}
