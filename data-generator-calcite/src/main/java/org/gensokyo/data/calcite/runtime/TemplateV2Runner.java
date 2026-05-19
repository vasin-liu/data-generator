package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.writer.WriterVO;

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

        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(template.getExecutionPolicy());
        TemplateV2RuntimeRegistry registry = runtimeRegistryProvider.current();
        if ("IN_MEMORY".equals(policy.mode())) {
            return new InMemoryPipeline(this::createSink).run(template, policy, registry);
        }
        throw new UnsupportedOperationException("Execution mode not yet supported: " + policy.mode());
    }

    protected RowSink createSink(TemplateV2RuntimeRegistry runtimeRegistry, WriterVO writer) {
        return runtimeRegistry.createSink(writer);
    }

    protected RowSink createSink(WriterVO writer) {
        return runtimeRegistryProvider.current().createSink(writer);
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
