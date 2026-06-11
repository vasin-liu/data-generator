package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;

import org.gensokyo.data.model.v2.TransformVO;

public interface V2TransformFactory {
    boolean supports(TransformVO transform);

    CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context);
}
