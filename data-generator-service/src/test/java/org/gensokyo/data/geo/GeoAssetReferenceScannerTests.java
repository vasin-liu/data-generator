/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.util.RandomKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Reference-scan coverage for geo asset delete guard (GEO-09 / D-08).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class GeoAssetReferenceScannerTests {

    private static final UUID ASSET_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Autowired
    private GeoAssetReferenceScanner scanner;

    @Autowired
    private TemplateRepository templateRepository;

    @Test
    @Transactional
    void findUsages_boundaryAssetIdField_returnsUsage() {
        saveJsonTemplate(
                "tpl-boundary-asset",
                """
                        {
                          "name": "tpl-boundary-asset",
                          "sources": {
                            "geo": {
                              "type": "geo_synthetic",
                              "mode": "BOUNDARY_POINTS",
                              "boundaryAssetId": "%s",
                              "count": 3
                            }
                          }
                        }
                        """.formatted(ASSET_ID));

        List<GeoAssetTemplateUsage> usages = scanner.findUsages(ASSET_ID);

        Assertions.assertEquals(1, usages.size());
        Assertions.assertEquals("tpl-boundary-asset", usages.getFirst().templateName());
    }

    @Test
    @Transactional
    void findUsages_boundaryPathAssetWireFormat_returnsUsage() {
        saveJsonTemplate(
                "tpl-boundary-path",
                """
                        {
                          "name": "tpl-boundary-path",
                          "sources": {
                            "geo": {
                              "type": "geo_synthetic",
                              "mode": "BOUNDARY_POINTS",
                              "boundaryPath": "asset:%s",
                              "count": 3
                            }
                          }
                        }
                        """.formatted(ASSET_ID));

        List<GeoAssetTemplateUsage> usages = scanner.findUsages(ASSET_ID);

        Assertions.assertEquals(1, usages.size());
        Assertions.assertEquals("tpl-boundary-path", usages.getFirst().templateName());
    }

    @Test
    @Transactional
    void findUsages_unrelatedClasspathPath_returnsEmpty() {
        saveJsonTemplate(
                "tpl-classpath-only",
                """
                        {
                          "name": "tpl-classpath-only",
                          "sources": {
                            "geo": {
                              "type": "geo_synthetic",
                              "mode": "BOUNDARY_POINTS",
                              "boundaryPath": "classpath:geo/南沙区边界.geojson",
                              "count": 3
                            }
                          }
                        }
                        """);

        Assertions.assertTrue(scanner.findUsages(ASSET_ID).isEmpty());
    }

    @Test
    @Transactional
    void findUsages_yamlOnlyTemplate_detectsReference() {
        TemplatePO row = baseRow("tpl-yaml-only");
        row.setContentYaml("""
                name: tpl-yaml-only
                sources:
                  geo:
                    type: geo_synthetic
                    mode: BOUNDARY_POINTS
                    boundaryAssetId: %s
                    count: 2
                """.formatted(ASSET_ID));
        templateRepository.saveAndFlush(row);

        List<GeoAssetTemplateUsage> usages = scanner.findUsages(ASSET_ID);

        Assertions.assertEquals(1, usages.size());
        Assertions.assertEquals("tpl-yaml-only", usages.getFirst().templateName());
    }

    @Test
    @Transactional
    void findUsages_multipleTemplates_listsAll() {
        saveJsonTemplate(
                "tpl-a",
                """
                        {
                          "name": "tpl-a",
                          "sources": {
                            "g": {
                              "type": "geojson",
                              "assetId": "%s"
                            }
                          }
                        }
                        """.formatted(ASSET_ID));
        saveJsonTemplate(
                "tpl-b",
                """
                        {
                          "name": "tpl-b",
                          "sources": {
                            "g": {
                              "type": "geojson",
                              "path": "asset:%s"
                            }
                          }
                        }
                        """.formatted(ASSET_ID));

        List<GeoAssetTemplateUsage> usages = scanner.findUsages(ASSET_ID);

        Assertions.assertEquals(2, usages.size());
        Assertions.assertTrue(usages.stream().anyMatch(u -> "tpl-a".equals(u.templateName())));
        Assertions.assertTrue(usages.stream().anyMatch(u -> "tpl-b".equals(u.templateName())));
    }

    private void saveJsonTemplate(String name, String contentJson) {
        TemplatePO row = baseRow(name);
        row.setContentJson(contentJson);
        templateRepository.saveAndFlush(row);
    }

    private static TemplatePO baseRow(String name) {
        TemplatePO row = new TemplatePO();
        row.setId(RandomKit.snowFlake().nextId());
        row.setName(name);
        row.setArchived(Boolean.FALSE);
        row.setStatus("PUBLISHED");
        return row;
    }
}
