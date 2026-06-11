package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

import java.nio.file.Path;
import java.util.List;

public record TemplateV2RuntimeContext(RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver,
                                       TemplateV2RuntimeServices runtimeServices,
                                       List<Path> pluginDirectories,
                                       ClassLoader pluginClassLoader) {
    public TemplateV2RuntimeContext {
        runtimeServices = runtimeServices == null ? new TemplateV2RuntimeServices(null, null, null) : runtimeServices;
        pluginDirectories = pluginDirectories == null ? List.of() : List.copyOf(pluginDirectories);
        pluginClassLoader = pluginClassLoader == null
                ? TemplateV2RuntimeContext.class.getClassLoader()
                : pluginClassLoader;
    }

    public static TemplateV2RuntimeContext empty() {
        return new TemplateV2RuntimeContext(new NoopRuntimeJdbcEndpointResolver(), new TemplateV2RuntimeServices(null, null, null), List.of(),
                TemplateV2RuntimeContext.class.getClassLoader());
    }
}
