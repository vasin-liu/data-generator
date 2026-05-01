package org.gensokyo.data.calcite;

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
        for (V2SourceFactory factory : sourceFactories) {
            if (factory.supports(source)) {
                return factory.create(name, source);
            }
        }
        throw new UnsupportedOperationException("Unsupported V2 source in current runner: " + source.getClass().getSimpleName());
    }

    public CalciteRowTransformer.TransformResult applyTransform(TransformVO transform, CalciteExecutionContext context) {
        for (V2TransformFactory factory : transformFactories) {
            if (factory.supports(transform)) {
                return factory.apply(transform, context);
            }
        }
        throw new UnsupportedOperationException("Unsupported V2 transformer in current runner: " + transform.getClass().getSimpleName());
    }

    public RowSink createSink(WriterVO writer) {
        for (V2SinkFactory factory : sinkFactories) {
            if (factory.supports(writer)) {
                return factory.create(writer);
            }
        }
        throw new UnsupportedOperationException("Unsupported V2 sink writer in current runner: " + writer.getClass().getSimpleName());
    }
}
