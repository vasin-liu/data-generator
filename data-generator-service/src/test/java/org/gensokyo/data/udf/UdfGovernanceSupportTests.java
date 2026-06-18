/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Unit tests for {@link UdfGovernanceSupport} Java-plugin JAR validation (D-23).
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
class UdfGovernanceSupportTests {

    @Test
    void rejectsNonJarJavaPluginPayload() {
        UdfRecord record = javaPlugin("not a jar".getBytes(StandardCharsets.UTF_8));
        List<UdfValidationError> errors = UdfGovernanceSupport.check(record, true);
        Assertions.assertTrue(errors.stream().anyMatch(e -> "UDF_JAR_INVALID".equals(e.code())));
    }

    @Test
    void rejectsJarWithoutManifest() throws IOException {
        UdfRecord record = javaPlugin(jar(false));
        List<UdfValidationError> errors = UdfGovernanceSupport.check(record, true);
        Assertions.assertTrue(errors.stream().anyMatch(e -> "UDF_JAR_MANIFEST_MISSING".equals(e.code())));
    }

    @Test
    void acceptsJarWithManifest() throws IOException {
        UdfRecord record = javaPlugin(jar(true));
        List<UdfValidationError> errors = UdfGovernanceSupport.check(record, true);
        Assertions.assertTrue(errors.isEmpty());
    }

    private static UdfRecord javaPlugin(byte[] payload) {
        return new UdfRecord.Builder()
                .udfId("com.example.plugin")
                .version("1.0.0")
                .type(UdfType.JAVA_PLUGIN)
                .state(UdfLifecycleState.DRAFT)
                .payload(payload)
                .metadata(Map.of())
                .build();
    }

    private static byte[] jar(boolean withManifest) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            if (withManifest) {
                zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("org/example/Udf.class"));
            zip.write(new byte[] {1, 2, 3});
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
