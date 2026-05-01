package org.gensokyo.data.calcite;

import java.util.List;
import java.util.Objects;

public record DescriptorAwareTemplateV2RuntimePlugin(TemplateV2RuntimePlugin delegate,
                                                     TemplateV2RuntimePluginDescriptor descriptor)
        implements TemplateV2RuntimePlugin {
    public DescriptorAwareTemplateV2RuntimePlugin {
        delegate = Objects.requireNonNull(delegate, "delegate");
        descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }

    @Override
    public List<V2SourceFactory> sourceFactories() {
        return delegate.sourceFactories();
    }

    @Override
    public List<V2TransformFactory> transformFactories() {
        return delegate.transformFactories();
    }

    @Override
    public List<V2SinkFactory> sinkFactories() {
        return delegate.sinkFactories();
    }
}
