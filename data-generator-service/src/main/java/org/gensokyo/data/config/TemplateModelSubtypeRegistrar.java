package org.gensokyo.data.config;

import jakarta.annotation.PostConstruct;
import org.gensokyo.data.calcite.plugin.Pf4jRuntimeExtensionLocator;
import org.gensokyo.data.json.JsonSubtypeRegistry;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(Pf4jRuntimeExtensionLocator.class)
public class TemplateModelSubtypeRegistrar {
    private final Pf4jRuntimeExtensionLocator pf4jRuntimeExtensionLocator;

    public TemplateModelSubtypeRegistrar(Pf4jRuntimeExtensionLocator pf4jRuntimeExtensionLocator) {
        this.pf4jRuntimeExtensionLocator = pf4jRuntimeExtensionLocator;
    }

    @PostConstruct
    void registerAtStartup() {
        refresh();
    }

    public void refresh() {
        pf4jRuntimeExtensionLocator.refresh();
        for (ClassLoader classLoader : pf4jRuntimeExtensionLocator.pluginClassLoaders()) {
            JsonSubtypeRegistry.registerSubtypes(SourceVO.class, classLoader);
            JsonSubtypeRegistry.registerSubtypes(TransformVO.class, classLoader);
            JsonSubtypeRegistry.registerSubtypes(WriterVO.class, classLoader);
        }
    }
}
