/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * Unit tests for {@link DynamicKafkaTemplateRegistry} primary fallback and unknown cluster errors.
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
class DynamicKafkaTemplateRegistryTests {

  @Test
  void template_blankClusterUsesPrimary() {
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, String> primaryTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
    DynamicKafkaTemplateRegistry registry =
        new DynamicKafkaTemplateRegistry("primary", Map.of("primary", primaryTemplate));

    Assertions.assertSame(primaryTemplate, registry.template(null));
    Assertions.assertSame(primaryTemplate, registry.template(""));
    Assertions.assertSame(primaryTemplate, registry.template("   "));
  }

  @Test
  void template_unknownClusterThrowsIllegalArgumentException() {
    DynamicKafkaTemplateRegistry registry =
        new DynamicKafkaTemplateRegistry("primary", Map.of());

    IllegalArgumentException ex =
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.template("missing"));
    Assertions.assertTrue(ex.getMessage().contains("missing"));
  }
}
