package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.Locale;
import java.util.Map;

public class TemplateV2Runner {
    private final TemplateV2RuntimeRegistryProvider runtimeRegistryProvider;

    protected TemplateV2Runner() {
        this(new RefreshableTemplateV2RuntimeRegistryProvider(
                TemplateV2RuntimePlugins.loadProviders(),
                new TemplateV2RuntimeRegistryFactory(),
                TemplateV2RuntimeContext.empty()
        ));
    }

    public TemplateV2Runner(TemplateV2RuntimeRegistry runtimeRegistry) {
        this(new FixedTemplateV2RuntimeRegistryProvider(runtimeRegistry));
    }

    public TemplateV2Runner(TemplateV2RuntimeRegistryProvider runtimeRegistryProvider) {
        this.runtimeRegistryProvider = runtimeRegistryProvider;
    }

    public TemplateV2RunResult run(TemplateV2VO template) {
        if (template == null) {
            throw new IllegalArgumentException("Template V2 must not be null");
        }
        if (template.getTransformers().isEmpty()) {
            throw new IllegalArgumentException("Current V2 runner requires at least one transformer");
        }

        CalciteExecutionContext context = new CalciteExecutionContext();
        TemplateV2RuntimeRegistry runtimeRegistry = runtimeRegistryProvider.current();
        for (Map.Entry<String, SourceVO> entry : template.getSources().entrySet()) {
            context.addSource(runtimeRegistry.createSource(entry.getKey(), entry.getValue()));
        }

        CalciteRowTransformer.TransformResult current = null;
        for (TransformVO transformer : template.getTransformers()) {
            current = runtimeRegistry.applyTransform(transformer, context);
            context = new CalciteExecutionContext()
                    .addTable("input", current.schema(), current.rows())
                    .addTable("current", current.schema(), current.rows());
        }

        if (current == null) {
            throw new IllegalStateException("Current V2 runner produced no transform result");
        }

        writeSinks(runtimeRegistry, template, current);
        return new TemplateV2RunResult(current.schema(), current.rows());
    }

    private void writeSinks(TemplateV2RuntimeRegistry runtimeRegistry,
                            TemplateV2VO template,
                            CalciteRowTransformer.TransformResult result) {
        SinkPolicyMode mode = sinkPolicyMode(template.getSinkExecutionPolicy());
        int sinkIndex = 0;
        for (WriteStageVO sink : template.getSinks()) {
            int writerIndex = 0;
            for (WriterVO writer : sink.getWriters()) {
                try {
                    createSink(runtimeRegistry, writer).write(result.schema(), result.rows());
                } catch (RuntimeException e) {
                    if (mode == SinkPolicyMode.CONTINUE_ON_ERROR) {
                        writerIndex++;
                        continue;
                    }
                    throw sinkWriteFailure(sinkIndex, writerIndex, writer, e);
                }
                writerIndex++;
            }
            sinkIndex++;
        }
    }

    protected RowSink createSink(TemplateV2RuntimeRegistry runtimeRegistry, WriterVO writer) {
        return runtimeRegistry.createSink(writer);
    }

    protected RowSink createSink(WriterVO writer) {
        return runtimeRegistryProvider.current().createSink(writer);
    }

    private SinkPolicyMode sinkPolicyMode(SinkExecutionPolicyVO policy) {
        if (policy == null || policy.getMode() == null || policy.getMode().isBlank()) {
            return SinkPolicyMode.FAIL_FAST;
        }
        return SinkPolicyMode.valueOf(policy.getMode().trim().toUpperCase(Locale.ROOT));
    }

    private IllegalStateException sinkWriteFailure(int sinkIndex,
                                                   int writerIndex,
                                                   WriterVO writer,
                                                   RuntimeException cause) {
        return new IllegalStateException("Failed to execute Template V2 sink writer"
                + " at sink index [" + sinkIndex + "]"
                + ", writer index [" + writerIndex + "]"
                + ", type [" + writer.getType() + "]"
                + ", model [" + writer.getClass().getName() + "]"
                + ", target [" + writer.getTarget() + "]", cause);
    }

    private enum SinkPolicyMode {
        FAIL_FAST,
        CONTINUE_ON_ERROR
    }

    private static final class FixedTemplateV2RuntimeRegistryProvider implements TemplateV2RuntimeRegistryProvider {
        private final TemplateV2RuntimeRegistry runtimeRegistry;

        private FixedTemplateV2RuntimeRegistryProvider(TemplateV2RuntimeRegistry runtimeRegistry) {
            this.runtimeRegistry = runtimeRegistry;
        }

        @Override
        public TemplateV2RuntimeRegistry current() {
            return runtimeRegistry;
        }

        @Override
        public TemplateV2RuntimeRegistry refresh() {
            return runtimeRegistry;
        }
    }
}
