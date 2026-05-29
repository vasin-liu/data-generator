/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.writer.WriterVO;

/**
 * Entry point for Template V2 execution: resolves execution policy and delegates to the appropriate pipeline.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class TemplateV2Runner {
    private final TemplateV2RuntimeRegistryProvider runtimeRegistryProvider;

    /**
     * Creates a runner with the default refreshable runtime registry from classpath plugins.
     */
    protected TemplateV2Runner() {
        this(new RefreshableTemplateV2RuntimeRegistryProvider(
                TemplateV2RuntimePlugins.loadProviders(),
                new TemplateV2RuntimeRegistryFactory(),
                TemplateV2RuntimeContext.empty()
        ));
    }

    /**
     * Creates a runner backed by a fixed runtime registry (typical in tests).
     *
     * @param runtimeRegistry registry supplying sources, transforms, and sinks
     */
    public TemplateV2Runner(TemplateV2RuntimeRegistry runtimeRegistry) {
        this(new FixedTemplateV2RuntimeRegistryProvider(runtimeRegistry));
    }

    /**
     * Creates a runner that resolves the runtime registry from a provider (supports refresh).
     *
     * @param runtimeRegistryProvider supplies the active registry per run
     */
    public TemplateV2Runner(TemplateV2RuntimeRegistryProvider runtimeRegistryProvider) {
        this.runtimeRegistryProvider = runtimeRegistryProvider;
    }

    /**
     * Executes a Template V2 definition using the resolved execution policy mode.
     *
     * @param template template definition
     * @return run result including output schema, optional rows, and metrics
     * @throws IllegalArgumentException if the template is invalid or mode constraints are violated
     */
    public TemplateV2RunResult run(TemplateV2VO template) {
        if (template == null) {
            throw new IllegalArgumentException("Template V2 must not be null");
        }
        if (template.getTransformers().isEmpty()) {
            throw new IllegalArgumentException("Current V2 runner requires at least one transformer");
        }

        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(template.getExecutionPolicy());
        TemplateV2RuntimeRegistry registry = runtimeRegistryProvider.current();
        if ("STREAMING".equals(policy.mode())) {
            return new StreamingPipeline(this::createSink).run(template, policy, registry);
        }
        if ("IN_MEMORY".equals(policy.mode())) {
            return new InMemoryPipeline(this::createSink).run(template, policy, registry);
        }
        if ("CHUNKED".equals(policy.mode())) {
            return new ChunkedPipeline(this::createSink).run(template, policy, registry);
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
