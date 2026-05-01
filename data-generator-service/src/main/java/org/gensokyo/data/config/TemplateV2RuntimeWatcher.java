package org.gensokyo.data.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.calcite.TemplateV2RuntimeContext;
import org.gensokyo.data.calcite.TemplateV2RuntimeRegistryProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Component
@ConditionalOnProperty(prefix = DataGeneratorProperties.PREFIX, name = "v2-plugin-auto-refresh", havingValue = "true", matchIfMissing = true)
public class TemplateV2RuntimeWatcher {
    private final TemplateV2RuntimeContext runtimeContext;
    private final TemplateV2RuntimeRegistryProvider runtimeRegistryProvider;
    private final ObjectProvider<TemplateModelSubtypeRegistrar> subtypeRegistrarProvider;
    private final ObjectProvider<Templates> templatesProvider;
    private final List<WatchService> watchServices = new ArrayList<>();
    private ExecutorService executorService;

    public TemplateV2RuntimeWatcher(TemplateV2RuntimeContext runtimeContext,
                                    TemplateV2RuntimeRegistryProvider runtimeRegistryProvider,
                                    ObjectProvider<TemplateModelSubtypeRegistrar> subtypeRegistrarProvider,
                                    ObjectProvider<Templates> templatesProvider) {
        this.runtimeContext = runtimeContext;
        this.runtimeRegistryProvider = runtimeRegistryProvider;
        this.subtypeRegistrarProvider = subtypeRegistrarProvider;
        this.templatesProvider = templatesProvider;
    }

    @PostConstruct
    void start() {
        List<Path> directories = runtimeContext.pluginDirectories().stream()
                .filter(Files::isDirectory)
                .toList();
        if (directories.isEmpty()) {
            return;
        }
        executorService = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        for (Path directory : directories) {
            register(directory);
        }
    }

    @PreDestroy
    void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        for (WatchService watchService : watchServices) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }
        watchServices.clear();
    }

    private void register(Path directory) {
        try {
            WatchService watchService = directory.getFileSystem().newWatchService();
            directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            watchServices.add(watchService);
            executorService.submit(() -> watch(directory, watchService));
        } catch (IOException ignored) {
        }
    }

    private void watch(Path directory, WatchService watchService) {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                return;
            }

            boolean shouldRefresh = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                Object context = event.context();
                if (context instanceof Path changed && changed.toString().endsWith(".jar")) {
                    shouldRefresh = true;
                }
            }
            key.reset();
            if (shouldRefresh && Files.isDirectory(directory)) {
                TemplateModelSubtypeRegistrar subtypeRegistrar = subtypeRegistrarProvider.getIfAvailable();
                if (subtypeRegistrar != null) {
                    subtypeRegistrar.refresh();
                }
                runtimeRegistryProvider.refresh();
                Templates templates = templatesProvider.getIfAvailable();
                if (templates != null) {
                    templates.reloadAll();
                }
            }
        }
    }
}
