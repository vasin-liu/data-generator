package org.gensokyo.data.calcite;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CalciteExecutionFlowTests {

    @Test
    void transformsNumberIteratorRowsWithProjectionAndFilter() {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(1);
        iterator.setTo(5);
        iterator.setStep(1);

        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);

        CalciteExecutionContext context = new CalciteExecutionContext()
                .addSource(new IteratorRowSource("input", source));

        CalciteRowTransformer.TransformResult result = new CalciteRowTransformer(
                "SELECT value, value + 1 AS next_value FROM input WHERE value >= 3"
        ).transform(context);

        Assertions.assertEquals(2, result.schema().getColumns().size());
        Assertions.assertEquals(3, result.rows().size());
        Assertions.assertEquals("3", result.rows().get(0).getString("value"));
        Assertions.assertEquals("4", result.rows().get(0).getString("next_value"));
        Assertions.assertEquals("6", result.rows().get(2).getString("next_value"));
    }
}
