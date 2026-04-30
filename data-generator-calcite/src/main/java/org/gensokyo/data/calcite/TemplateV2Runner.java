package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemplateV2Runner {
    private final List<V2SourceFactory> sourceFactories;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TemplateV2Runner() {
        this(List.of(new IteratorSourceFactory()), null);
    }

    public TemplateV2Runner(List<V2SourceFactory> sourceFactories) {
        this(sourceFactories, null);
    }

    public TemplateV2Runner(List<V2SourceFactory> sourceFactories, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.sourceFactories = new ArrayList<>(sourceFactories);
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public TemplateV2RunResult run(TemplateV2VO template) {
        if (template == null) {
            throw new IllegalArgumentException("Template V2 must not be null");
        }
        if (template.getTransformers().isEmpty()) {
            throw new IllegalArgumentException("Current V2 runner requires at least one transformer");
        }

        CalciteExecutionContext context = new CalciteExecutionContext();
        for (Map.Entry<String, SourceVO> entry : template.getSources().entrySet()) {
            context.addSource(createSource(entry.getKey(), entry.getValue()));
        }

        CalciteRowTransformer.TransformResult current = null;
        for (TransformVO transformer : template.getTransformers()) {
            current = applyTransformer(transformer, context);
            context = new CalciteExecutionContext()
                    .addTable("input", current.schema(), current.rows())
                    .addTable("current", current.schema(), current.rows());
        }

        if (current == null) {
            throw new IllegalStateException("Current V2 runner produced no transform result");
        }

        writeSinks(template.getSinks(), current);
        return new TemplateV2RunResult(current.schema(), current.rows());
    }

    private RowSource createSource(String name, SourceVO source) {
        for (V2SourceFactory factory : sourceFactories) {
            if (factory.supports(source)) {
                return factory.create(name, source);
            }
        }
        throw new UnsupportedOperationException("Unsupported V2 source in current runner: " + source.getClass().getSimpleName());
    }

    private CalciteRowTransformer.TransformResult applyTransformer(TransformVO transformer, CalciteExecutionContext context) {
        if (transformer instanceof SqlTransformVO sqlTransform) {
            return new CalciteRowTransformer(sqlTransform.getSql()).transform(context);
        }
        throw new UnsupportedOperationException("Unsupported V2 transformer in current runner: " + transformer.getClass().getSimpleName());
    }

    private void writeSinks(List<WriteStageVO> sinks, CalciteRowTransformer.TransformResult result) {
        for (WriteStageVO sink : sinks) {
            for (WriterVO writer : sink.getWriters()) {
                createSink(writer).write(result.schema(), result.rows());
            }
        }
    }

    private RowSink createSink(WriterVO writer) {
        if (writer instanceof ConsoleWriterVO) {
            return new ConsoleRowSinkAdapter();
        }
        if (writer instanceof JdbcWriterVO jdbcWriter) {
            if (namedParameterJdbcTemplate == null) {
                throw new IllegalStateException("NamedParameterJdbcTemplate is required for JDBC sink support");
            }
            return new JdbcRowSinkAdapter(namedParameterJdbcTemplate, jdbcWriter);
        }
        throw new UnsupportedOperationException("Unsupported V2 sink writer in current runner: " + writer.getClass().getSimpleName());
    }
}
