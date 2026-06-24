/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.elasticsearch;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Unit tests for {@link DynamicElasticsearchClientRegistry} primary fallback and unknown cluster errors.
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
class DynamicElasticsearchClientRegistryTests {

  @Test
  void llc_blankClusterUsesPrimary() {
    RestClient primaryClient = org.mockito.Mockito.mock(RestClient.class);
    DynamicElasticsearchClientRegistry registry =
        new DynamicElasticsearchClientRegistry("primary", Map.of("primary", primaryClient));

    Assertions.assertSame(primaryClient, registry.llc(null));
    Assertions.assertSame(primaryClient, registry.llc(""));
    Assertions.assertSame(primaryClient, registry.llc("   "));
  }

  @Test
  void llc_unknownClusterThrowsIllegalArgumentException() {
    DynamicElasticsearchClientRegistry registry =
        new DynamicElasticsearchClientRegistry("primary", Map.of());

    IllegalArgumentException ex =
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.llc("missing"));
    Assertions.assertTrue(ex.getMessage().contains("missing"));
  }
}
