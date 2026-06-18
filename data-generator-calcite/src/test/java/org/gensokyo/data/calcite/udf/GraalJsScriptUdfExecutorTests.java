/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.udf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for {@link GraalJsScriptUdfExecutor} covering value conversion, sandboxing, and timeouts.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
class GraalJsScriptUdfExecutorTests {

    private final GraalJsScriptUdfExecutor executor = new GraalJsScriptUdfExecutor();

    // Generous budget absorbs GraalJS cold-start (interpreter-only) latency on the first eval.
    private static final int WARMUP_TOLERANT_TIMEOUT_MS = 10000;

    @Test
    void evaluatesStringResultFromArguments() {
        Object result = executor.execute("return 'hi ' + args[0];", List.of("bob"), WARMUP_TOLERANT_TIMEOUT_MS);
        Assertions.assertEquals("hi bob", result);
    }

    @Test
    void evaluatesNumericResult() {
        Object result = executor.execute("return args[0] + args[1];", List.of(2, 3), WARMUP_TOLERANT_TIMEOUT_MS);
        // GraalJS arithmetic yields a JS number; the registered return type drives final coercion.
        Assertions.assertInstanceOf(Number.class, result);
        Assertions.assertEquals(5L, ((Number) result).longValue());
    }

    @Test
    void deniesHostClassAccess() {
        // Java host access is blocked by the sandbox, so referencing Java.type must fail.
        Assertions.assertThrows(IllegalStateException.class, () ->
                executor.execute("return Java.type('java.lang.System').lineSeparator();", List.of(),
                        WARMUP_TOLERANT_TIMEOUT_MS));
    }

    @Test
    void enforcesTimeout() {
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () ->
                executor.execute("while (true) {}", List.of(), 200));
        Assertions.assertTrue(ex.getMessage().contains("timed out"));
    }

    @Test
    void rejectsBlankScript() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                executor.execute("   ", List.of(), 2000));
    }
}
