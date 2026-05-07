package org.gensokyo.data.calcite.sql;

import org.gensokyo.data.calcite.*;

import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TransformVO;

public class SqlTransformFactory implements V2TransformFactory {
    private final TemplateV2SqlFunctionRegistry sqlFunctionRegistry;

    public SqlTransformFactory() {
        this(TemplateV2SqlFunctionRegistry.builtIn());
    }

    public SqlTransformFactory(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
        this.sqlFunctionRegistry = sqlFunctionRegistry;
    }

    @Override
    public boolean supports(TransformVO transform) {
        return transform instanceof SqlTransformVO;
    }

    @Override
    public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
        SqlTransformVO sqlTransform = (SqlTransformVO) transform;
        return new CalciteRowTransformer(sqlTransform.getSql(), sqlFunctionRegistry).transform(context);
    }
}
