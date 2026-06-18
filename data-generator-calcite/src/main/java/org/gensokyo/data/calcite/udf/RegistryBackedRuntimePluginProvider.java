/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.udf;

import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeContext;
import org.gensokyo.data.calcite.TemplateV2RuntimePlugin;
import org.gensokyo.data.calcite.TemplateV2RuntimePluginDescriptor;
import org.gensokyo.data.calcite.TemplateV2RuntimePluginProvider;
import org.gensokyo.data.calcite.TemplateV2SqlFunction;
import org.gensokyo.data.calcite.sql.TemplateV2SqlFunctionRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bridges published registry UDFs into the Template V2 runtime as a synthetic plugin.
 *
 * <p>Re-reads the {@link RegistrySqlFunctionSource} on every {@code createPlugin} call so the
 * refreshable registry provider observes registry mutations after publish/deprecate (D-08).
 * Functions whose name collides with a built-in SQL function are dropped so built-ins win (D-07).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
public class RegistryBackedRuntimePluginProvider implements TemplateV2RuntimePluginProvider {

    private static final String PLUGIN_ID = "registry-udf-plugin";

    private final RegistrySqlFunctionSource functionSource;

    /**
     * @param functionSource live source of published registry SQL functions
     */
    public RegistryBackedRuntimePluginProvider(RegistrySqlFunctionSource functionSource) {
        this.functionSource = functionSource;
    }

    /**
     * Builds a snapshot plugin exposing the registry's currently published SQL functions.
     *
     * @param context runtime context (unused; registry UDFs need no runtime services)
     * @return plugin contributing registry-backed SQL functions
     */
    @Override
    public TemplateV2RuntimePlugin createPlugin(TemplateV2RuntimeContext context) {
        List<TemplateV2SqlFunction> published = functionSource.publishedSqlFunctions();
        List<TemplateV2SqlFunction> contributed = filterBuiltInCollisions(published);
        return new TemplateV2RuntimePlugin() {
            @Override
            public TemplateV2RuntimePluginDescriptor descriptor() {
                return TemplateV2RuntimePluginDescriptor.builder(PLUGIN_ID)
                        .version("1.0.0")
                        .hostVersionRange("current")
                        .provider("registry")
                        .build();
            }

            @Override
            public List<TemplateV2SqlFunction> sqlFunctions() {
                return contributed;
            }
        };
    }

    private static List<TemplateV2SqlFunction> filterBuiltInCollisions(List<TemplateV2SqlFunction> functions) {
        Set<String> builtInNames = TemplateV2SqlFunctionRegistry.builtIn().functions().stream()
                .map(function -> TemplateV2SqlFunctionRegistry.normalize(function.name()))
                .collect(Collectors.toSet());
        List<TemplateV2SqlFunction> retained = new ArrayList<>();
        for (TemplateV2SqlFunction function : functions) {
            // Built-in functions take precedence; registry entries reusing their name are skipped.
            if (!builtInNames.contains(TemplateV2SqlFunctionRegistry.normalize(function.name()))) {
                retained.add(function);
            }
        }
        return List.copyOf(retained);
    }
}
