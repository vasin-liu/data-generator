package org.gensokyo.data.calcite;

import org.pf4j.DefaultPluginManager;

import java.nio.file.Path;
import java.util.List;

public class PathBasedPf4jRuntimeExtensionLocator extends PluginManagerPf4jRuntimeExtensionLocator {
    public PathBasedPf4jRuntimeExtensionLocator(List<Path> pluginDirectories) {
        super(new DefaultPluginManager(resolveRoot(pluginDirectories)));
    }

    private static Path resolveRoot(List<Path> pluginDirectories) {
        if (pluginDirectories == null || pluginDirectories.isEmpty()) {
            return Path.of("plugins");
        }
        return pluginDirectories.getFirst();
    }
}
