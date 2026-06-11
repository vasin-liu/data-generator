/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ScaleLimitExceededException}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class ScaleLimitExceededExceptionTests {

    @Test
    void messageIncludesPolicyFieldAndStage() {
        ScaleLimitExceededException ex = new ScaleLimitExceededException(
                "maxRowsInMemory", 100, 101, "SOURCE_READ", "orders");
        Assertions.assertTrue(ex.getMessage().contains("maxRowsInMemory"));
        Assertions.assertTrue(ex.getMessage().contains("SOURCE_READ"));
        Assertions.assertTrue(ex.getMessage().contains("orders"));
    }
}
