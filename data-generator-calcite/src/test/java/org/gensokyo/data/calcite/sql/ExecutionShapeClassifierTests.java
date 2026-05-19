/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for {@link ExecutionShapeClassifier}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class ExecutionShapeClassifierTests {

    @Test
    void singleTableSelectIsRowLocal() {
        ExecutionShape shape = ExecutionShapeClassifier.classify(
                "SELECT id, name FROM orders WHERE status = 'OPEN'");
        Assertions.assertEquals(ExecutionShape.ROW_LOCAL, shape);
    }

    @Test
    void groupByIsMaterializationRequired() {
        ExecutionShape shape = ExecutionShapeClassifier.classify(
                "SELECT status, COUNT(*) FROM orders GROUP BY status");
        Assertions.assertEquals(ExecutionShape.MATERIALIZATION_REQUIRED, shape);
    }

    @Test
    void innerJoinIsMaterializationRequiredInPhase2aClassifier() {
        ExecutionShape shape = ExecutionShapeClassifier.classify(
                "SELECT o.id, c.name FROM orders o INNER JOIN customers c ON o.customer_id = c.id");
        Assertions.assertEquals(ExecutionShape.MATERIALIZATION_REQUIRED, shape);
    }

    @Test
    void classifyTemplateUsesFirstSqlTransform() {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT id FROM orders");
        TemplateV2VO template = new TemplateV2VO();
        template.setTransformers(List.of(transform));

        Assertions.assertEquals(ExecutionShape.ROW_LOCAL, ExecutionShapeClassifier.classify(template));
    }

    @Test
    void classifyTemplateWithoutSqlThrows() {
        TemplateV2VO template = new TemplateV2VO();
        template.setTransformers(List.of(new SqlTransformVO()));

        Assertions.assertThrows(IllegalArgumentException.class, () -> ExecutionShapeClassifier.classify(template));
    }
}
