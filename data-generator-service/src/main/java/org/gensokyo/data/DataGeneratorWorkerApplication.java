/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone distributed worker process entrypoint (Phase C2).
 *
 * <p>Run with profile {@code distributed-worker} so only the worker poller claims queue rows;
 * coordinator enqueue remains on the primary service node.</p>
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Slf4j
@SpringBootApplication
public class DataGeneratorWorkerApplication {

    /**
     * Starts the worker JVM with the distributed-worker Spring profile.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DataGeneratorWorkerApplication.class);
        application.setAdditionalProfiles("distributed-worker");
        application.run(args);
        log.info("Distributed worker process started (profile=distributed-worker)");
    }
}
