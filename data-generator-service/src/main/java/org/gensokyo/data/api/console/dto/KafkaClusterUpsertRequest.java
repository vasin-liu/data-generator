/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;
import java.util.Map;

/**
 * Request body for console-managed Kafka cluster upsert.
 *
 * @param name             unique cluster id
 * @param bootstrapServers broker addresses
 * @param clientId         optional producer client id
 * @param acks             optional producer acks
 * @param compressionType  optional producer compression
 * @param retries          optional producer retries
 * @param securityProtocol optional {@code security.protocol}
 * @param saslMechanism    optional {@code sasl.mechanism}
 * @param saslJaasConfig   optional {@code sasl.jaas.config}; blank on edit keeps existing
 * @param properties       optional extra producer/client properties
 */
public record KafkaClusterUpsertRequest(
        String name,
        List<String> bootstrapServers,
        String clientId,
        String acks,
        String compressionType,
        Integer retries,
        String securityProtocol,
        String saslMechanism,
        String saslJaasConfig,
        Map<String, String> properties) {
}
