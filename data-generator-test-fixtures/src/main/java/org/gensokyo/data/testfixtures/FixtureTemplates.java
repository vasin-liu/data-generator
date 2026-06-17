/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.testfixtures;

import org.gensokyo.data.exception.DataGeneratorException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads Template V2 fixture YAML from the classpath by scenario name.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
public final class FixtureTemplates {

    private static final String TEMPLATE_PREFIX = "fixtures/templates/";

    private FixtureTemplates() {
    }

    /**
     * Reads the YAML template for the given scenario from the classpath.
     *
     * @param scenario scenario file basename without extension (for example {@code reader-jdbc-basic})
     * @return non-empty YAML content
     * @throws IllegalArgumentException when the scenario resource is missing
     */
    public static String load(String scenario) {
        String resource = TEMPLATE_PREFIX + scenario + ".yaml";
        try (InputStream input = FixtureTemplates.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing fixture template scenario: " + scenario);
            }
            String yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            if (yaml.isBlank()) {
                throw new IllegalArgumentException("Fixture template scenario is empty: " + scenario);
            }
            return yaml;
        }
        catch (IOException ex) {
            throw new DataGeneratorException("Failed to read fixture template [" + scenario + "]", ex);
        }
    }
}
