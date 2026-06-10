/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link V2ScenarioCatalogService}.
 *
 * @author Gensokyo
 * @since 2026-06-02
 */
class V2ScenarioCatalogServiceTest {

    private final V2ScenarioCatalogService service = new V2ScenarioCatalogService(new JacksonParser());

    @Test
    void listOfficial_returnsFiveFamilies() {
        var rows = service.listOfficial();
        Assertions.assertEquals(5, rows.size());
        Assertions.assertTrue(rows.stream().anyMatch(row -> "GF-A".equals(row.scenarioId())));
        Assertions.assertTrue(rows.stream().anyMatch(row -> "GF-B".equals(row.scenarioId())));
        Assertions.assertTrue(rows.stream().anyMatch(row -> "GF-WF".equals(row.scenarioId())));
        Assertions.assertTrue(rows.stream().anyMatch(row -> "GF-WFS".equals(row.scenarioId())));
        Assertions.assertTrue(rows.stream().anyMatch(row -> "GF-JS".equals(row.scenarioId())));
    }

    @Test
    void loadDraft_seedsUniqueName() {
        var first = service.loadDraft("GF-A");
        var second = service.loadDraft("GF-A");
        Assertions.assertNotNull(first.getName());
        Assertions.assertNotNull(second.getName());
        Assertions.assertNotEquals(first.getName(), second.getName());
        Assertions.assertTrue(first.getSources() != null && !first.getSources().isEmpty());
    }

    @Test
    void loadDraft_unknownIdRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.loadDraft("GF-UNKNOWN"));
    }
}
