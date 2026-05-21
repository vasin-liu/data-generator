package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.ArrayList;
import java.util.List;

public class TemplateV2RuntimeRegistry {
    private final List<V2SourceFactory> sourceFactories;
    private final List<V2TransformFactory> transformFactories;
    private final List<V2SinkFactory> sinkFactories;

    public TemplateV2RuntimeRegistry(List<V2SourceFactory> sourceFactories,
                                     List<V2TransformFactory> transformFactories,
                                     List<V2SinkFactory> sinkFactories) {
        this.sourceFactories = new ArrayList<>(sourceFactories);
        this.transformFactories = new ArrayList<>(transformFactories);
        this.sinkFactories = new ArrayList<>(sinkFactories);
    }

    public RowSource createSource(String name, SourceVO source) {
        return createSource(name, source, null);
    }

    /**
     * Creates a row source, passing execution policy to JDBC factories that support chunked reads.
     *
     * @param name   logical source name
     * @param source source configuration
     * @param policy optional effective execution policy for chunked JDBC reads
     * @return row source
     */
    public RowSource createSource(String name, SourceVO source, EffectiveExecutionPolicy policy) {
        for (V2SourceFactory factory : sourceFactories) {
            if (factory.supports(source)) {
                try {
                    RowSource rowSource;
                    if (policy != null && factory instanceof QuerySourceFactory queryFactory) {
                        rowSource = queryFactory.create(name, source, policy);
                    } else if (policy != null && factory instanceof PostGisQuerySourceFactory postGisFactory) {
                        rowSource = postGisFactory.create(name, source, policy);
                    } else {
                        rowSource = factory.create(name, source);
                    }
                    if (source.getPolicy() == null) {
                        return rowSource;
                    }
                    return new SourcePolicyRowSource(rowSource, source.getPolicy());
                } catch (RuntimeException e) {
                    throw runtimeFailure("source", source.getType(), source.getClass(), factory.getClass(), e);
                }
            }
        }
        throw new UnsupportedOperationException("Unsupported V2 source in current runner: " + source.getClass().getSimpleName());
    }

    public CalciteRowTransformer.TransformResult applyTransform(TransformVO transform, CalciteExecutionContext context) {
        for (V2TransformFactory factory : transformFactories) {
            if (factory.supports(transform)) {
                try {
                    return factory.apply(transform, context);
                } catch (RuntimeException e) {
                    throw runtimeFailure("transform", transform.getType(), transform.getClass(), factory.getClass(), e);
                }
            }
        }
        throw new UnsupportedOperationException("Unsupported V2 transformer in current runner: " + transform.getClass().getSimpleName());
    }

    public RowSink createSink(WriterVO writer) {
        for (V2SinkFactory factory : sinkFactories) {
            if (factory.supports(writer)) {
                try {
                    return factory.create(writer);
                } catch (RuntimeException e) {
                    throw runtimeFailure("sink", writer.getType(), writer.getClass(), factory.getClass(), e);
                }
            }
        }
        throw new UnsupportedOperationException("Unsupported V2 sink writer in current runner: " + writer.getClass().getSimpleName());
    }

    private IllegalStateException runtimeFailure(String nodeKind,
                                                 String nodeType,
                                                 Class<?> modelClass,
                                                 Class<?> factoryClass,
                                                 RuntimeException cause) {
        return new IllegalStateException("Failed to execute Template V2 " + nodeKind
                + " factory [" + factoryClass.getName() + "] for type [" + nodeType + "]"
                + " and model [" + modelClass.getName() + "]", cause);
    }
}
