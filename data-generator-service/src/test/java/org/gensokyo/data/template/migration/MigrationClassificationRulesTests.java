/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for {@link MigrationClassificationRules}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class MigrationClassificationRulesTests {

    @Test
    void exactWhenCountsMatchAndSampleRateHigh() {
        MigrationClassification c = MigrationClassificationRules.classify(
                1000, 1000, 0.999, List.of());
        Assertions.assertEquals(MigrationClassification.EXACT, c);
    }

    @Test
    void blockedWhenSampleRateLow() {
        MigrationClassification c = MigrationClassificationRules.classify(
                1000, 1000, 0.90, List.of());
        Assertions.assertEquals(MigrationClassification.BLOCKED, c);
    }

    @Test
    void approximateWhenWarningsNonEmptyAndRateHigh() {
        MigrationClassification c = MigrationClassificationRules.classify(
                1000, 1000, 0.99, List.of("SourcePolicyVO approximation"));
        Assertions.assertEquals(MigrationClassification.APPROXIMATE, c);
    }

    @Test
    void exactWhenOnlyInfoWarningsPresent() {
        MigrationClassification c = MigrationClassificationRules.classify(
                1000, 1000, 0.999, List.of("INFO: optional note"));
        Assertions.assertEquals(MigrationClassification.EXACT, c);
    }

    @Test
    void blockedWhenCountsMismatchAndSampleRateBetweenThresholds() {
        MigrationClassification c = MigrationClassificationRules.classify(
                1000, 900, 0.98, List.of());
        Assertions.assertEquals(MigrationClassification.BLOCKED, c);
    }

    @Test
    void approximateWhenCountsMismatchButSampleRateExact() {
        MigrationClassification c = MigrationClassificationRules.classify(
                1000, 900, 0.999, List.of());
        Assertions.assertEquals(MigrationClassification.APPROXIMATE, c);
    }
}
