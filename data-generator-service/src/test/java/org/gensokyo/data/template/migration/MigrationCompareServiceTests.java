/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link MigrationCompareService}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class MigrationCompareServiceTests {

    @Test
    void classifiesExactWhenStubRunsMatch() {
        MigrationCompareService service = new MigrationCompareService(stubExecutorMatching());
        TemplateVO v1 = new TemplateVO();
        v1.setId(1L);
        v1.setName("stub-v1");
        TemplateV2VO v2 = new TemplateV2VO();
        v2.setId(1L);
        v2.setName("stub-v2");

        MigrationComparisonReport report = service.compare(v1, v2, MigrationCompareOptions.defaults());

        Assertions.assertEquals(MigrationClassification.EXACT, report.getClassification());
        Assertions.assertEquals("accept", report.getRecommendation());
        Assertions.assertEquals(3, report.getV1RowCount());
        Assertions.assertEquals(3, report.getV2RowCount());
        Assertions.assertEquals(1.0d, report.getSampleMatchRate(), 0.0001d);
    }

    private static TemplateRunExecutor stubExecutorMatching() {
        List<Map<String, Object>> rows = List.of(
                row("id", 1L),
                row("id", 2L),
                row("id", 3L));
        return new TemplateRunExecutor() {
            @Override
            public RunOutcome runV1(TemplateVO v1, Map<String, Object> params, MigrationCompareOptions options) {
                return new RunOutcome(rows.size(), rows);
            }

            @Override
            public RunOutcome runV2(TemplateV2VO v2, Map<String, Object> params, MigrationCompareOptions options) {
                return new RunOutcome(rows.size(), rows);
            }
        };
    }

    private static Map<String, Object> row(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}
