/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;

/**
 * Stores JDBC driver JARs and registers drivers with {@link DriverManager}.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
public class DataSourceDriverSupport {

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
     * Registers a JDBC driver from a JAR path when the path is non-blank.
     *
     * @param jarFilePath absolute jar path
     * @param driverClassName driver class name
     * @throws Exception when load fails
     */
    public void registerDriverFromJar(String jarFilePath, String driverClassName) throws Exception {
        if (jarFilePath == null || jarFilePath.isBlank()) {
            return;
        }
        URL jarUrl = Path.of(jarFilePath).toUri().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[] {jarUrl});
        Class<?> driverClass = Class.forName(driverClassName, true, loader);
        DriverManager.registerDriver((java.sql.Driver) driverClass.getDeclaredConstructor().newInstance());
    }
}
