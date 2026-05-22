/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link V1SpelExpressionRewriter}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class V1SpelExpressionRewriterTests {

    @Test
    void rewritesDatasetPropertyAccess() {
        Assertions.assertEquals(
                "#row['parking_lot_name']",
                V1SpelExpressionRewriter.rewrite("#dataset.PARKING_LOT_NAME"));
    }

    @Test
    void rewritesDatasetBracketAccess() {
        Assertions.assertEquals("#row['id']", V1SpelExpressionRewriter.rewrite("#dataset['ID']"));
    }

    @Test
    void rewritesBareDatasetToRow() {
        Assertions.assertEquals("#row", V1SpelExpressionRewriter.rewrite("#dataset"));
    }

    @Test
    void preservesFakerReferences() {
        String rewritten = V1SpelExpressionRewriter.rewrite("#faker.number.numberBetween(1,#dataset)");
        Assertions.assertTrue(rewritten.contains("#faker"));
        Assertions.assertTrue(rewritten.contains("#row"));
    }
}
