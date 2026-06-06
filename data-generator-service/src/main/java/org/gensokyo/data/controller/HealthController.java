/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight liveness endpoint for packaged deployments and keepalive scripts.
 *
 * <p>{@code opcode=0} is retained for compatibility with legacy health-check scripts.</p>
 *
 * @author Gensokyo
 * @since 2026-06-06
 */
@RestController
public class HealthController {

    @Value("${spring.application.name:data-generator-service}")
    private String applicationName;

    /**
     * Returns UP when the Spring context is ready to serve HTTP.
     *
     * @return health payload with {@code opcode=0} and {@code status=UP}
     */
    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("opcode", 0);
        body.put("status", "UP");
        body.put("application", applicationName);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
