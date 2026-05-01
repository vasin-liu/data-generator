package org.gensokyo.data.calcite;

import java.util.List;

public interface Pf4jRuntimeExtensionLocator extends AutoCloseable {
    default void start() {
    }

    default void refresh() {
        close();
        start();
    }

    List<Pf4jTemplateV2RuntimeExtension> loadExtensions();

    default List<ClassLoader> pluginClassLoaders() {
        return List.of();
    }

    @Override
    default void close() {
    }
}
