/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.support;

import org.testcontainers.DockerClientFactory;

/**
 * Detects whether Docker is available for Testcontainers-backed service-module ITs.
 * <p>
 * Copied from calcite's test-scoped {@code DockerTestSupport} because that helper is not on
 * the {@code data-generator-service} test classpath.
 *
 * @author Gensokyo
 * @since 2026-07-25
 */
public final class DockerTestSupport {

    private DockerTestSupport() {
    }

    /**
     * @return {@code true} when Testcontainers can reach a Docker daemon
     */
    public static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        }
        catch (RuntimeException | LinkageError ex) {
            return false;
        }
    }
}
