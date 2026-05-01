package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TransformVO;

public class SqlTransformFactory implements V2TransformFactory {
    @Override
    public boolean supports(TransformVO transform) {
        return transform instanceof SqlTransformVO;
    }

    @Override
    public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
        SqlTransformVO sqlTransform = (SqlTransformVO) transform;
        return new CalciteRowTransformer(sqlTransform.getSql()).transform(context);
    }
}
