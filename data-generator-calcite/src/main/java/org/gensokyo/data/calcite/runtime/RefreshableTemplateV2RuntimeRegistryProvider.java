package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RefreshableTemplateV2RuntimeRegistryProvider implements TemplateV2RuntimeRegistryProvider {
    private final List<TemplateV2RuntimePluginProvider> pluginProviders;
    private final TemplateV2RuntimeRegistryFactory registryFactory;
    private final TemplateV2RuntimeContext runtimeContext;
    private final AtomicReference<TemplateV2RuntimeRegistry> current = new AtomicReference<>();

    public RefreshableTemplateV2RuntimeRegistryProvider(List<TemplateV2RuntimePluginProvider> pluginProviders,
                                                        TemplateV2RuntimeRegistryFactory registryFactory,
                                                        TemplateV2RuntimeContext runtimeContext) {
        this.pluginProviders = List.copyOf(pluginProviders);
        this.registryFactory = registryFactory;
        this.runtimeContext = runtimeContext;
        try {
            this.current.set(registryFactory.fromProviders(this.pluginProviders, runtimeContext));
        } catch (RuntimeException e) {
            throw new TemplateV2RuntimeRegistryBuildException("Failed to initialize Template V2 runtime registry", e);
        }
    }

    @Override
    public TemplateV2RuntimeRegistry current() {
        return current.get();
    }

    @Override
    public TemplateV2RuntimeRegistry refresh() {
        try {
            TemplateV2RuntimeRegistry refreshed = registryFactory.fromProviders(pluginProviders, runtimeContext);
            current.set(refreshed);
            return refreshed;
        } catch (RuntimeException e) {
            throw new TemplateV2RuntimeRegistryBuildException("Failed to refresh Template V2 runtime registry", e);
        }
    }
}
