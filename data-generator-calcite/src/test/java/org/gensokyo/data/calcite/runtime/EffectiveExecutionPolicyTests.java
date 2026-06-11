/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EffectiveExecutionPolicy}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class EffectiveExecutionPolicyTests {

    @Test
    void defaultsToInMemoryWithRepositoryDefaults() {
        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(null);

        Assertions.assertEquals("IN_MEMORY", policy.mode());
        Assertions.assertEquals(500_000, policy.maxRowsInMemory());
        Assertions.assertEquals(5_000, policy.sourceChunkSize());
        Assertions.assertEquals(1_000, policy.sinkBatchSize());
        Assertions.assertEquals(100, policy.previewRowLimit());
        Assertions.assertTrue(policy.failOnLimitExceeded());
        Assertions.assertEquals(50_000, policy.broadcastMaxRows());
        Assertions.assertNull(policy.maxTotalRows());
    }

    @Test
    void overlaysTemplatePolicy() {
        ExecutionPolicyVO vo = new ExecutionPolicyVO();
        vo.setMode("CHUNKED");
        vo.setMaxRowsInMemory(1000);
        vo.setSourceChunkSize(200);
        vo.setSinkBatchSize(50);

        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(vo);

        Assertions.assertEquals("CHUNKED", policy.mode());
        Assertions.assertEquals(1000, policy.maxRowsInMemory());
        Assertions.assertEquals(200, policy.sourceChunkSize());
        Assertions.assertEquals(50, policy.sinkBatchSize());
        Assertions.assertEquals(100, policy.previewRowLimit());
        Assertions.assertTrue(policy.failOnLimitExceeded());
        Assertions.assertEquals(100, policy.broadcastMaxRows());
    }

    @Test
    void overlaysBroadcastMaxRowsFromTemplate() {
        ExecutionPolicyVO vo = new ExecutionPolicyVO();
        vo.setMode("CHUNKED");
        vo.setMaxRowsInMemory(1_000_000);
        vo.setBroadcastMaxRows(25_000);

        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(vo);

        Assertions.assertEquals(25_000, policy.broadcastMaxRows());
    }

    @Test
    void exposesMaxTotalRowsWhenConfigured() {
        ExecutionPolicyVO vo = new ExecutionPolicyVO();
        vo.setMaxTotalRows(10_000);

        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(vo);

        Assertions.assertEquals(10_000, policy.maxTotalRows());
    }
}
