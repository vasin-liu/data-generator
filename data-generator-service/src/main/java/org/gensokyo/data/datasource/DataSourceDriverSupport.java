/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.exception.DataGeneratorException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Stores optional uploaded JDBC JARs and loads drivers via isolated class loaders when bundled.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@RequiredArgsConstructor
public class DataSourceDriverSupport {

    private final BundledJdbcDriverRegistry bundledDrivers;

    /**
     * Saves an uploaded driver JAR under {@code uploaded-drivers/}.
     *
     * @param driverFile multipart upload
     * @return absolute path to saved jar
     * @throws IOException when transfer fails
     */
    public String storeDriverJar(MultipartFile driverFile) throws IOException {
        String fileName = driverFile.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            fileName = "driver.jar";
        }
        Path destFile = Paths.get(System.getProperty("user.dir"), "..", "uploaded-drivers", fileName);
        Path parent = destFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        driverFile.transferTo(destFile);
        return destFile.toAbsolutePath().toString();
    }

    /**
     * Loads a JDBC driver using uploaded JAR, bundled preset JARs, or the application classpath (last resort).
     *
     * @param driverClassName preferred driver class
     * @param jdbcUrl         JDBC URL for preset / bundle matching
     * @param jarFilePath     optional user-uploaded JAR path
     * @return load result with isolated loader when bundled or uploaded
     */
    public JdbcDriverLoadResult ensureDriverLoaded(String driverClassName, String jdbcUrl, String jarFilePath) {
        List<String> candidates = JdbcDriverPresetCatalog.resolveDriverClassCandidates(driverClassName, jdbcUrl);
        if (jarFilePath != null && !jarFilePath.isBlank()) {
            return loadFromUploadedJar(jarFilePath, candidates);
        }
        Optional<String> bundleKey = JdbcDriverPresetCatalog.resolveBundleKey(driverClassName, jdbcUrl);
        if (bundleKey.isPresent() && bundledDrivers.hasBundle(bundleKey.get())) {
            return loadFromBundle(bundleKey.get(), candidates);
        }
        return loadFromApplicationClasspath(candidates);
    }

    /**
     * Opens a JDBC connection without registering drivers on the global {@link java.sql.DriverManager}.
     *
     * @param url      JDBC URL
     * @param username user
     * @param password password
     * @param loaded   prior {@link #ensureDriverLoaded} result
     * @return open connection
     * @throws Exception when connect fails
     */
    public Connection openConnection(String url, String username, String password, JdbcDriverLoadResult loaded)
            throws Exception {
        Driver driver = instantiateDriver(loaded);
        Properties props = new Properties();
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        Connection connection = driver.connect(url, props);
        if (connection == null) {
            throw new IllegalStateException("Driver returned null connection for URL: " + url);
        }
        return connection;
    }

    private JdbcDriverLoadResult loadFromUploadedJar(String jarFilePath, List<String> candidates) {
        Exception lastFailure = null;
        for (String candidate : candidates) {
            try {
                URLClassLoader loader = new URLClassLoader(
                        new URL[] {Path.of(jarFilePath).toUri().toURL()},
                        ClassLoader.getPlatformClassLoader());
                instantiateDriver(candidate, loader);
                return new JdbcDriverLoadResult(candidate, loader, false);
            } catch (Exception e) {
                lastFailure = e;
            }
        }
        throw failure(candidates, lastFailure);
    }

    private JdbcDriverLoadResult loadFromBundle(String bundleKey, List<String> candidates) {
        URLClassLoader loader = bundledDrivers.classLoaderFor(bundleKey)
                .orElseThrow(() -> new DataGeneratorException("Bundled JDBC driver missing: " + bundleKey));
        Exception lastFailure = null;
        for (String candidate : candidates) {
            try {
                instantiateDriver(candidate, loader);
                return new JdbcDriverLoadResult(candidate, loader, true);
            } catch (Exception e) {
                lastFailure = e;
            }
        }
        throw failure(candidates, lastFailure);
    }

    private JdbcDriverLoadResult loadFromApplicationClasspath(List<String> candidates) {
        Exception lastFailure = null;
        for (String candidate : candidates) {
            try {
                Class.forName(candidate);
                return new JdbcDriverLoadResult(candidate, Thread.currentThread().getContextClassLoader(), false);
            } catch (Exception e) {
                lastFailure = e;
            }
        }
        throw failure(candidates, lastFailure);
    }

    private static Driver instantiateDriver(JdbcDriverLoadResult loaded) throws Exception {
        return instantiateDriver(loaded.driverClassName(), loaded.classLoader());
    }

    private static Driver instantiateDriver(String driverClassName, ClassLoader loader) throws Exception {
        Class<?> driverClass = Class.forName(driverClassName, true, loader);
        return (Driver) driverClass.getDeclaredConstructor().newInstance();
    }

    private static DataGeneratorException failure(List<String> candidates, Exception lastFailure) {
        String message = "Failed to load JDBC driver"
                + (candidates.isEmpty() ? "" : " (tried: " + String.join(", ", candidates) + ")");
        return new DataGeneratorException(message, lastFailure);
    }
}
