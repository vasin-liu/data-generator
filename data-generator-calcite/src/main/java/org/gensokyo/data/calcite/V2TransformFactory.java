package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.TransformVO;

public interface V2TransformFactory {
    boolean supports(TransformVO transform);

    CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context);
}
