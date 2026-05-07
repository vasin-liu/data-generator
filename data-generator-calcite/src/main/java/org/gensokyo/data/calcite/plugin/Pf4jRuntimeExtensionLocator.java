package org.gensokyo.data.calcite.plugin;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;

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
