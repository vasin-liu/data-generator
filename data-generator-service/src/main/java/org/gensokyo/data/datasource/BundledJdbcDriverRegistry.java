/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Isolated {@link URLClassLoader} instances over JDBC driver JARs shipped under {@code jdbc-bundled/}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Slf4j
@Component
public class BundledJdbcDriverRegistry {

    private static final String BUNDLED_PREFIX = "jdbc-bundled/";

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final Path extractRoot;
    private final Map<String, URLClassLoader> loaders = new ConcurrentHashMap<>();

    /**
     * Resolves bundled driver directories from the filesystem (assembly layout) or classpath (dev).
     */
    public BundledJdbcDriverRegistry() {
        this.extractRoot = initExtractRoot();
        log.info("JDBC bundled driver root: {}", extractRoot);
    }

    /**
     * @param bundleKey bundle directory name (e.g. mysql, kingbase8)
     * @return true when at least one JAR is available for the bundle
     */
    public boolean hasBundle(String bundleKey) {
        if (bundleKey == null || bundleKey.isBlank()) {
            return false;
        }
        return !listJarUrls(bundleKey).isEmpty();
    }

    /**
     * @param bundleKey bundle directory name
     * @return isolated class loader, or empty when the bundle is missing
     */
    public java.util.Optional<URLClassLoader> classLoaderFor(String bundleKey) {
        if (!hasBundle(bundleKey)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(loaders.computeIfAbsent(bundleKey, this::createLoader));
    }

    private URLClassLoader createLoader(String bundleKey) {
        List<URL> jarUrls = listJarUrls(bundleKey);
        if (jarUrls.isEmpty()) {
            throw new IllegalStateException("No bundled JDBC jars for bundle: " + bundleKey);
        }
        // Platform parent avoids delegating driver classes to the application classpath.
        return new URLClassLoader(jarUrls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    }

    private List<URL> listJarUrls(String bundleKey) {
        List<URL> urls = new ArrayList<>();
        Path fsDir = resolveFilesystemBundleDir(bundleKey);
        if (fsDir != null && Files.isDirectory(fsDir)) {
            try (Stream<Path> paths = Files.list(fsDir)) {
                paths.filter(p -> p.toString().endsWith(".jar"))
                        .sorted(Comparator.comparing(Path::getFileName))
                        .forEach(p -> urls.add(toUrl(p)));
            } catch (IOException e) {
                log.warn("Failed to list filesystem bundled jars for {}: {}", bundleKey, e.getMessage());
            }
        }
        if (!urls.isEmpty()) {
            return urls;
        }
        return extractClasspathBundle(bundleKey);
    }

    private Path resolveFilesystemBundleDir(String bundleKey) {
        List<Path> candidates = List.of(
                Path.of(System.getProperty("user.dir"), "jdbc-bundled", bundleKey),
                Path.of(System.getProperty("user.dir"), "..", "jdbc-bundled", bundleKey),
                Path.of(System.getProperty("user.dir"), "conf", "jdbc-bundled", bundleKey));
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private List<URL> extractClasspathBundle(String bundleKey) {
        List<URL> urls = new ArrayList<>();
        try {
            Resource[] resources =
                    resourceResolver.getResources("classpath:" + BUNDLED_PREFIX + bundleKey + "/*.jar");
            Path bundleDir = extractRoot.resolve(bundleKey);
            Files.createDirectories(bundleDir);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                String fileName = resource.getFilename();
                if (fileName == null || !fileName.endsWith(".jar")) {
                    continue;
                }
                Path target = bundleDir.resolve(fileName);
                if (!Files.exists(target)) {
                    try (InputStream in = resource.getInputStream()) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                urls.add(target.toUri().toURL());
            }
        } catch (IOException e) {
            log.debug("Classpath bundled JDBC not found for {}: {}", bundleKey, e.getMessage());
        }
        return urls;
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid jar path: " + path, e);
        }
    }

    private static Path initExtractRoot() {
        try {
            Path dir = Path.of(System.getProperty("java.io.tmpdir"), "data-generator-jdbc-bundled");
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create JDBC bundle extract directory", e);
        }
    }
}
