package org.gensokyo.data.calcite;

import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;

public class PluginManagerPf4jRuntimeExtensionLocator implements Pf4jRuntimeExtensionLocator {
    private final PluginManager pluginManager;
    private boolean started;

    public PluginManagerPf4jRuntimeExtensionLocator(PluginManager pluginManager) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager");
    }

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        started = true;
    }

    @Override
    public List<Pf4jTemplateV2RuntimeExtension> loadExtensions() {
        return List.copyOf(pluginManager.getExtensions(Pf4jTemplateV2RuntimeExtension.class));
    }

    @Override
    public List<ClassLoader> pluginClassLoaders() {
        List<ClassLoader> classLoaders = new ArrayList<>();
        for (PluginWrapper plugin : pluginManager.getStartedPlugins()) {
            if (plugin.getPluginClassLoader() != null) {
                classLoaders.add(plugin.getPluginClassLoader());
            }
        }
        return List.copyOf(classLoaders);
    }

    @Override
    public synchronized void close() {
        if (!started) {
            return;
        }
        try {
            pluginManager.stopPlugins();
        } catch (ConcurrentModificationException ignored) {
            // PF4J stop order may iterate over a list that changes during shutdown in tests.
            // Plugins are still test-scoped here, so tolerate shutdown instability rather than fail the host path.
        }
        started = false;
    }
}
