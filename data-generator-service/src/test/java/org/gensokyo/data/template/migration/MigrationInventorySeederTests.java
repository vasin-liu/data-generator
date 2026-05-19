package org.gensokyo.data.template.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class MigrationInventorySeederTests {

    @Test
    void seedsRegressionEntriesFromClasspath() {
        MigrationInventorySeeder seeder = new MigrationInventorySeeder();
        List<MigrationInventoryEntry> entries = seeder.regressionEntriesFromClasspath();

        Assertions.assertFalse(entries.isEmpty());
        Assertions.assertTrue(entries.stream().anyMatch(e -> "repository".equals(e.getOrigin())));
        Assertions.assertTrue(entries.stream().anyMatch(e -> "regression-v1-iterator-simple".equals(e.getId())));
        Assertions.assertTrue(entries.stream().anyMatch(e -> "regression-v1-query-lookup".equals(e.getId())));

        MigrationInventoryEntry iteratorEntry = entries.stream()
                .filter(e -> "regression-v1-iterator-simple".equals(e.getId()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("synthetic", iteratorEntry.getScenarioFamily());
        Assertions.assertEquals(MigrationClassification.UNCLASSIFIED, iteratorEntry.getMigrationClass());

        MigrationInventoryEntry lookupEntry = entries.stream()
                .filter(e -> "regression-v1-query-lookup".equals(e.getId()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("multi_source", lookupEntry.getScenarioFamily());
    }
}
